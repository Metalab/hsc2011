
// (cd vmcode/ && ./build.py)
// Using http://svn.clifford.at/tools/trunk/arduino-cc.sh:
// arduino-cc -P /dev/ttyACM0 -X 57600 firmware.cc hardware.cc RF12.cpp OneWire.cpp embedvm.c firmware_vm.cc firmware_evt.cc firmware_ser.cc firmware_net.cc

#include <WProgram.h>
#include <avr/eeprom.h>

#include "OneWire.h"
#include "hardware.h"

#include "firmware.h"

bool do_soft_reset;
bool pending_login;
bool got_ibutton;

/************************************************************
 *                         Main Loop                        *
 ************************************************************/

bool poll_ibutton()
{
	byte ds_addr[8];
	ds.reset_search();
	if (ds.search(ds_addr) == 1)
	{
		buzzer(440);
		Serial.print("* OneWire Addr: ");
		ser_printmac(ds_addr);
		Serial.println("");
		memcpy(ib_addr, ds_addr, 8);
		do_soft_reset = true;
		got_ibutton = true;
		return true;
	}
	return false;
}

/* called from the main loop when contact with the base station is lost */
void reset_soft()
{
	Serial.println("* Performing soft reset.");

	do_soft_reset = false;
	pending_login = true;

	hw_reset_soft();
	evt_button_mask = 0;
	evt_button_last = 0;

	randomSeed(random(0xffff) ^ millis());
	randomSeed(random(0xffff) ^ (my_addr[1] << 8 | my_addr[0]));
	randomSeed(random(0xffff) ^ (my_addr[3] << 8 | my_addr[2]));
	randomSeed(random(0xffff) ^ (my_addr[5] << 8 | my_addr[4]));
	randomSeed(random(0xffff) ^ (my_addr[7] << 8 | my_addr[6]));

	vm_reset();

	/* TBD: send LOGIN if a MAC address is configured either from eeprom or
	 * from an iButton -- use modified form or modify net_send_until_acked
	 * so serial communication stays possible */
}

void setup()
{
	got_ibutton = false;

	for (int i=0; i<8; i++)
		my_addr[i] = eeprom_read_byte((uint8_t*)i);
	for (int i=0; i<8; i++)
		base_addr[i] = eeprom_read_byte((uint8_t*)(8+i));

	vm.mem_read = &vm_mem_read;
	vm.mem_write = &vm_mem_write;
	vm.call_user = &vm_call_user;

	hw_setup();
	reset_soft();

	Serial.println("");
	Serial.println("=== 3.14159265358979323846264338327950288419716939937510 ===");
}

void loop()
{
	ser_poll();

	if (poll_ibutton())
		return;

	if (do_soft_reset)
		reset_soft();

	if (pending_login)
	{
		if (!memcmp(my_addr, base_addr, 8)) {
			Serial.println("* Running as basestation.");
			pending_login = false;
			basestation = true;
			return;
		}

		Serial.print("* Logging in as ");
		ser_printmac(my_addr);
		Serial.print(" on base station ");
		ser_printmac(base_addr);
		Serial.println(".");

		sendbuf.hdr.pkttype = 'L';
		sendbuf.hdr.seqnum = ++last_send_seq;
		memcpy(&sendbuf.hdr.dst, &base_addr, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_login);
		memcpy(sendbuf.pkt_login.ibutton, ib_addr, 8);


		embedvm_interrupt(&vm, EMBEDVM_SYM_kitt);
		vm_running = true;

		net_send_until_acked('l');
		pending_login = false;
		return;
	}
	else
	{
		uint16_t timeout = millis() - last_ping;
		if (!basestation && timeout > NET_PING_TIMEOUT) {
			sendbuf.hdr.pkttype = 'E';
			sendbuf.hdr.seqnum = ++last_send_seq;
			memcpy(&sendbuf.hdr.dst, &base_addr, 8);
			memcpy(&sendbuf.hdr.src, &my_addr, 8);
			sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_event);
			sendbuf.pkt_event.event_type = ET_PING;
			sendbuf.pkt_event.event_payload = timeout;
			net_send_until_acked('e');
			return;
		}
		if (net_poll())
			net_proc();
		evt_poll();
		// check iButton - TBD

		vm_step(true);
	}
}

