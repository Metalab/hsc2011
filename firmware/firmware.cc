
// Using http://svn.clifford.at/tools/trunk/arduino-cc.sh:
// arduino-cc -P /dev/ttyACM0 -X 57600 firmware.cc hardware.cc RF12.cpp OneWire.cpp embedvm.c

#include <WProgram.h>
#include <avr/eeprom.h>
#include <avr/pgmspace.h>

#include "RF12.h"
#include "OneWire.h"
#include "pktspec.h"
#include "hardware.h"
#include "embedvm.h"

// resend interval in ms from the first attempt to send; chosen arbitrarily
int16_t net_resend_delays[] = { 20, 80, 200, 300, 400, 500, 500 };
#define NET_PING_TIMEOUT 30000

bool basestation;
uint8_t ib_addr[8];
uint8_t my_addr[8];
uint8_t base_addr[8];
uint16_t last_send_seq;
uint16_t last_ping;

uint8_t sendbuf_len;
struct pktbuffer_s sendbuf;
struct pktbuffer_s recvbuf;

bool do_soft_reset;
bool pending_login;
bool got_ibutton;

void net_proc();
bool net_poll();
void net_send();
bool net_send_until_acked(uint8_t ack_type);

uint8_t ser_readbyte();
void ser_endecho();
void ser_readeol();
uint8_t ser_readhex();
uint16_t ser_readhex16();
void ser_readmac(uint8_t *buf);
bool ser_readbool();
int ser_readtri();
int ser_readeventtype();

void ser_printhex(uint8_t val);
void ser_printhex16(uint16_t val);
void ser_printmac(uint8_t *buf);
void ser_printpkt(struct pktbuffer_s *pkt);

void ser_poll();
bool poll_ibutton();

void vm_reset();

uint8_t evt_button_mask; // [ MSB keyup3, .., keyup0, keydown3, .., keydown0 LSB ]
uint8_t evt_button_last; // bit number i is true if button i was last held down

bool vm_running;
bool vm_stop_next;
struct embedvm_s vm = { };
// virtual memory layout is vm_mem starting at 0, immediately followed by vm_rom
uint8_t vm_mem[256] = { };
uint8_t vm_rom[256] PROGMEM = {
	0x93, 0x91, 0xb1, // set leds 0 and 1 on, the others off (one argument, value 3)
	0x98, 0xff, 0x90, 0x92, 0xb2, // set red part of rgb led (two arguments, 0 and 255)
	0x91, 0x91, 0xb2, // get blue value of rgb led
	0x91, 0xb5, // send that value as a user event
	0x90, 0xb0, // stop
};

/************************************************************
 *                          Network                         *
 ************************************************************/

void net_proc()
{
	ser_printpkt(&recvbuf);

	switch (recvbuf.hdr.pkttype)
	{
	case 'S':
		if (recvbuf.pkt_status.vm_stop)
			vm_running = false;
		if (recvbuf.pkt_status.vm_start)
			vm_running = true;
		if (recvbuf.pkt_status.vm_start && recvbuf.pkt_status.vm_stop)
			vm_stop_next = true;
		if (recvbuf.pkt_status.set_ip)
		{
			// this overwrites options previously set from
			// vm_start/stop. this is desired -- starting and
			// setting ip at the same time would cause the VM
			// to run twice if acks are lost.
			//
			// not using interrupt here as no "return" is
			// meaningful.
			vm_reset();
			vm.ip = recvbuf.pkt_status.ip_val;
		}

		if (recvbuf.pkt_status.set_rgb) {
			rgb(0, recvbuf.pkt_status.rgb_val[0]);
			rgb(1, recvbuf.pkt_status.rgb_val[1]);
			rgb(2, recvbuf.pkt_status.rgb_val[2]);
		}
		if (recvbuf.pkt_status.set_buzzer)
			buzzer(recvbuf.pkt_status.buzzer_val);
		for (uint8_t i = 0; i < 4; ++i)
			if (recvbuf.pkt_status.set_leds & (1<<i))
				led(i, recvbuf.pkt_status.leds_val & (1<<i));
		evt_button_mask &=~ recvbuf.pkt_status.eventmask_setbits; // clear all bits that are to be set
		evt_button_mask |= recvbuf.pkt_status.eventmask_val & recvbuf.pkt_status.eventmask_setbits;

		sendbuf.hdr.pkttype = 's';
		sendbuf.hdr.seqnum = recvbuf.hdr.seqnum;
		memcpy(&sendbuf.hdr.dst, &recvbuf.hdr.src, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_status_ack);

		sendbuf.pkt_status_ack.vm_running = vm_running;
		sendbuf.pkt_status_ack.ip = vm.ip;
		sendbuf.pkt_status_ack.leds = getled(0) | (getled(1) << 1) | (getled(2) << 2) | (getled(3) << 3);
		sendbuf.pkt_status_ack.buttons = button(0) | (button(1) << 1) | (button(2) << 2) | (button(3) << 3);
		sendbuf.pkt_status_ack.buzzer = getbuzzer();
		sendbuf.pkt_status_ack.rgb[0] = getrgb(0);
		sendbuf.pkt_status_ack.rgb[1] = getrgb(1);
		sendbuf.pkt_status_ack.rgb[2] = getrgb(2);
		sendbuf.pkt_status_ack.eventmask = evt_button_mask;

		// send just once -- if the ack gets lost, the host
		// will send another 'S', and it has to be idempotent
		// anyway.
		net_send();
		break;
	case 'L':
	case 'E':
		// immediately ack -- no processing further than
		// reporting to serial is required
		sendbuf.hdr.pkttype = recvbuf.hdr.pkttype == 'E' ? 'e' : 'l';
		sendbuf.hdr.seqnum = recvbuf.hdr.seqnum;
		memcpy(&sendbuf.hdr.dst, &recvbuf.hdr.src, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s);

		// send only once -- client will re-transmit the same
		// event, we'll ack it then, and it's up to the
		// software side to know that it was a retransmit.
		net_send();
		break;
	case 's':
	case 'w':
	case 'r':
		// does not need special processing - those events
		// typically affect the base station which just needs
		// the serial output; they need no acking as they are
		// acks themselves.
		break;
	case 'e':
	case 'l':
		// should never be needed -- this is sent from base to
		// device and is already blocked on in net_send.
		Serial.println("* Received ack that was expected to be already processed.");
		break;
	case 'W':
	case 'R':
		/* TBD, fall through */
	case 'X':
		// ack reset
		sendbuf.hdr.pkttype = 'x';
		sendbuf.hdr.seqnum = recvbuf.hdr.seqnum;
		memcpy(&sendbuf.hdr.dst, &recvbuf.hdr.src, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s);
		net_send();
		do_soft_reset = true;
		break;
	default:
		Serial.println("* Received unknown command, not processed.");
	}
}

bool net_poll()
{
	// a new pkt to process?
	if (!rf12_recvDone() || rf12_crc != 0)
		return false;

	// does it have the right magic?
	if (memcmp((void*)rf12_data, HEADER_MAGIC, HEADER_MAGIC_LENGTH))
		return false;

	// is it for us?
	if (memcmp(&my_addr, ((struct pktbuffer_s*)rf12_data)->hdr.dst, 8))
		return false;

	// copy to recv buffer
	memcpy(&recvbuf, (void*)rf12_data, rf12_len);

	return true;
}

void net_send()
{
	// drop every incoming until we can send
	while (!rf12_canSend())
		rf12_recvDone();

	/* write magic */
	memcpy(sendbuf.hdr.magic, HEADER_MAGIC, HEADER_MAGIC_LENGTH);

	// copy to RFM12 lib and transmit
	rf12_sendStart(0, &sendbuf, sendbuf_len);

	Serial.print("* just sent: ");
	ser_printpkt(&sendbuf);
}

/* like net_send, but will try again and again until an ack of ack_type is
 * received. drops all other packages. if a timeout is reached, it returns
 * false and sets a flag to do a soft reset when the main loop is next run.
 *
 * this can block for an extended period of time and runs a mini-mainloop
 * inside.
 * */
bool net_send_until_acked(uint8_t ack_type)
{
	uint16_t delta_t;
	uint32_t first_send;
	uint8_t resend_number = 0;
	uint8_t resend_extra_delay = random(net_resend_delays[0]);

	net_send();
	first_send = millis();
	
	while (1)
	{
		if (net_poll()) {
			// a 'net_proc light' as it only waits for a very
			// particular package
			if (recvbuf.hdr.pkttype == ack_type && recvbuf.hdr.seqnum == last_send_seq)
			{
				Serial.println("* Acknowledgement received, continuing.");
				if (!basestation)
					last_ping = millis();
				return true;
			}
		}
		delta_t = (uint16_t)(millis() - first_send);
		if (delta_t > net_resend_delays[resend_number]) {
			resend_extra_delay = random(net_resend_delays[++resend_number]);
			if (resend_number == sizeof(net_resend_delays)/sizeof(*net_resend_delays)) {
				Serial.println("* Giving up resending, starting soft-reset.");
				do_soft_reset = true;
				return false;
			} else {
				net_send();
				first_send = millis();
			}
		}
		if (poll_ibutton())
			return false;
	}
}


/************************************************************
 *                        Serial I/O                        *
 ************************************************************/

bool ser_echo;
bool ser_noecho;
bool ser_goteol;
uint8_t ser_unget_char;

uint8_t ser_readbyte()
{
	int c;
	if (ser_goteol)
		return '\n';
	if (ser_unget_char != 0) {
		c = ser_unget_char;
		ser_unget_char = 0;
		return c;
	}
	while ((c = Serial.read()) == -1) { }
	if (c == '\n' || c == '\r') {
		ser_goteol = true;
		return '\n';
	}
	if (ser_echo && !ser_noecho)
		Serial.write(c);
	return c;
}

void ser_endecho()
{
	if (ser_echo && !ser_noecho)
		Serial.println(ser_goteol ? "" : "...");
	ser_echo = false;
}

void ser_readeol()
{
	while (!ser_goteol)
		ser_readbyte();
	ser_endecho();
}

uint8_t ser_readhex()
{
	uint8_t val = 0;
	for (uint8_t i = 0; i < 2; i++) {
		char ch = ser_readbyte();
		if (ch == '\n')
			return 0;
		if (ch >= '0' && ch <= '9')
			val = (val << 4) | (ch - '0');
		else if (ch >= 'a' && ch <= 'f')
			val = (val << 4) | (0xa + ch - 'a');
		else if (ch >= 'A' && ch <= 'F')
			val = (val << 4) | (0xa + ch - 'A');
		else
			i--;
	}
	return val;
}

uint16_t ser_readhex16()
{
	uint16_t val = ser_readhex() << 8;
	val |= ser_readhex();
	return val;
}

void ser_readmac(uint8_t *buf)
{
	while (1)
	{
		char cmd = ser_readbyte();
		if (cmd == ' ' || cmd == '\t') {
			continue;
		} else if (cmd == '*') {
			memcpy(buf, my_addr, 8);
		} else if (cmd == '$') {
			memcpy(buf, base_addr, 8);
		} else {
			ser_unget_char = cmd;
			for (int i = 0; i < 8; i++)
				buf[i] = ser_readhex();
		}
		return;
	}
}

bool ser_readbool()
{
	while (1) {
		char ch = ser_readbyte();
		if (ch == '\n' || ch == 'n')
			return false;
		if (ch == 'y')
			return true;
	}
}

int ser_readtri()
{
	while (1) {
		char ch = ser_readbyte();
		if (ch == '\n' || ch == 'n')
			return 0;
		if (ch == 'y')
			return 1;
		if (ch == 'z')
			return -1;
	}
}

int ser_readeventtype()
{
	while (1) {
		char ch = ser_readbyte();
		switch(ch)
		{
		default:
			ser_goteol = true;
			/* fall through */
		case ET_BUTTON:
		case ET_PING:
		case ET_USER:
			return ch;
		case ' ':
		case '\t':
			continue;
		}
	}
}

void ser_printhex(uint8_t val)
{
	Serial.write("0123456789ABCDEF"[val >> 4]);
	Serial.write("0123456789ABCDEF"[val & 15]);
}

void ser_printhex16(uint16_t val)
{
	ser_printhex(val >> 8);
	ser_printhex(val & 255);
}

void ser_printmac(uint8_t *buf)
{
	for (int i=0; i < 8; i++)
		ser_printhex(buf[i]);
}

void ser_printpkt(struct pktbuffer_s *pkt)
{
	Serial.write(pkt->hdr.pkttype);
	Serial.write(' ');
	ser_printhex(pkt->hdr.seqnum);
	Serial.write(' ');

	ser_printmac(pkt->hdr.src);
	Serial.write(' ');

	ser_printmac(pkt->hdr.dst);
	Serial.write(' ');

	switch (pkt->hdr.pkttype)
	{
	case 'L':
		ser_printmac(pkt->pkt_login.ibutton);
		break;

	case 'l':
		break;

	case 'E':
		Serial.write(pkt->pkt_event.event_type);
		ser_printhex16(pkt->pkt_event.event_payload);
		break;

	case 'e':
		break;

	case 'S':
		Serial.write(pkt->pkt_status.vm_stop ? 'y' : 'n');
		Serial.write(pkt->pkt_status.vm_start ? 'y' : 'n');
		Serial.write(' ');
		
		Serial.write(pkt->pkt_status.set_ip ? 'y' : 'n');
		if (pkt->pkt_status.set_ip)
			ser_printhex16(pkt->pkt_status.ip_val);
		Serial.write(' ');
		
		Serial.write(pkt->pkt_status.set_rgb ? 'y' : 'n');
		if (pkt->pkt_status.set_rgb) {
			ser_printhex(pkt->pkt_status.rgb_val[0]);
			ser_printhex(pkt->pkt_status.rgb_val[1]);
			ser_printhex(pkt->pkt_status.rgb_val[2]);
		}
		Serial.write(' ');
		
		Serial.write(pkt->pkt_status.set_buzzer ? 'y' : 'n');
		if (pkt->pkt_status.set_buzzer)
			ser_printhex16(pkt->pkt_status.buzzer_val);
		Serial.write(' ');

		for (int i=0; i<4; i++) {
			if ((pkt->pkt_status.set_leds & (1 << i)) == 0)
				Serial.write('z');
			else if ((pkt->pkt_status.leds_val & (1 << i)) == 0)
				Serial.write('y');
			else
				Serial.write('n');
		}
		Serial.write(' ');
		
		ser_printhex(pkt->pkt_status.eventmask_setbits);
		ser_printhex(pkt->pkt_status.eventmask_val);
		break;

	case 's':
		Serial.write(pkt->pkt_status_ack.vm_running ? 'y' : 'n');
		Serial.write(' ');

		for (int i=0; i<4; i++) {
			if ((pkt->pkt_status_ack.leds & (1 << i)) != 0)
				Serial.write('y');
			else
				Serial.write('n');
		}
		Serial.write(' ');

		for (int i=0; i<4; i++) {
			if ((pkt->pkt_status_ack.buttons & (1 << i)) != 0)
				Serial.write('y');
			else
				Serial.write('n');
		}
		Serial.write(' ');

		ser_printhex16(pkt->pkt_status_ack.ip);
		Serial.write(' ');

		ser_printhex16(pkt->pkt_status_ack.buzzer);
		Serial.write(' ');

		for (int i=0; i < 3; i++)
			ser_printhex(pkt->pkt_status_ack.rgb[i]);
		Serial.write(' ');

		ser_printhex16(pkt->pkt_status_ack.eventmask);
		break;

	case 'W':
	case 'w':
	case 'R':
	case 'r':
		/* TBD */
		break;

	case 'X':
	case 'x':
		break;
	}

	Serial.println("");
}

void ser_poll()
{
	if (!Serial.available())
		return;

	ser_goteol = false;
	ser_unget_char = 0;
	char cmd = ser_readbyte();

	if (cmd == '-') {
		ser_noecho = false;
		Serial.println("* Terminal echo ENABLED.");
		return;
	}

	if (cmd == '+') {
		ser_noecho = true;
		Serial.println("* Terminal echo DISABLED.");
		return;
	}

	if (cmd == '=') {
		Serial.print("=== ");
		while ((cmd = ser_readbyte()) != '\n')
			Serial.write(cmd);
		Serial.println(" ===");
		return;
	}

	if (!ser_noecho) {
		Serial.write('-');
		Serial.write(cmd);
		ser_echo = true;
	}

	switch (cmd)
	{
	case 'M':
		switch (ser_readhex())
		{
		case 0:
			if (ser_goteol)
				break;

			ser_endecho();
			{
				uint8_t buf[8];

				Serial.print("* 1: my mac          = ");
				ser_printmac(my_addr);
				Serial.println();
				
				Serial.print("* 2: base mac        = ");
				ser_printmac(base_addr);
				Serial.println();

				for (int i=0; i<8; i++)
					buf[i] = eeprom_read_byte((uint8_t*)i);
				Serial.print("* 3: eeprom my mac   = ");
				ser_printmac(buf);
				Serial.println();

				for (int i=0; i<8; i++)
					buf[i] = eeprom_read_byte((uint8_t*)(8+i));
				Serial.print("* 4: eeprom base mac = ");
				ser_printmac(buf);
				Serial.println();
			}
			break;
			
		case 1:
			ser_readmac(my_addr);
			break;
			
		case 2:
			ser_readmac(base_addr);
			break;

		case 3:
			for (int i=0; i<8; i++)
				eeprom_write_byte((uint8_t*)i, my_addr[i]);
			break;

		case 4:
			for (int i=0; i<8; i++)
				eeprom_write_byte((uint8_t*)(8+i), base_addr[i]);
			break;

		case 5:
			memcpy(my_addr, base_addr, 8);
			break;

		default:
			goto parser_error;
		}
		break;

	case 'L':
	case 'l':
	case 'E':
	case 'e':
	case 'S':
	case 's':
	case 'W':
	case 'w':
	case 'R':
	case 'r':
	case 'X':
	case 'x':
		/* set pkttype */
		sendbuf.hdr.pkttype = cmd;

		/* parse seq num */
		sendbuf.hdr.seqnum = ser_readhex();

		/* parse src and dest mac addr */
		ser_readmac(sendbuf.hdr.src);
		ser_readmac(sendbuf.hdr.dst);

		/* size of generic pkt header */
		sendbuf_len = sizeof(struct pktbuffer_hdr_s);
		
		switch (cmd)
		{
		case 'L':
			sendbuf_len += sizeof(sendbuf.pkt_login);
			memcpy(sendbuf.pkt_login.ibutton, ib_addr, 8);
			break;
		case 'l':
			break;
		case 'E':
			sendbuf_len += sizeof(sendbuf.pkt_event);
			sendbuf.pkt_event.event_type = ser_readeventtype();
			sendbuf.pkt_event.event_payload = ser_readhex16();
			break;
		case 'e':
			break;
		case 'S':
			sendbuf_len += sizeof(sendbuf.pkt_status);

			sendbuf.pkt_status.vm_stop = ser_readbool();
			sendbuf.pkt_status.vm_start = ser_readbool();

			sendbuf.pkt_status.set_ip = ser_readbool();
			if (sendbuf.pkt_status.set_ip)
				sendbuf.pkt_status.ip_val = ser_readhex16();

			sendbuf.pkt_status.set_rgb = ser_readbool();
			if (sendbuf.pkt_status.set_rgb) {
				sendbuf.pkt_status.rgb_val[0] = ser_readhex();
				sendbuf.pkt_status.rgb_val[1] = ser_readhex();
				sendbuf.pkt_status.rgb_val[2] = ser_readhex();
			}

			sendbuf.pkt_status.set_buzzer = ser_readbool();
			if (sendbuf.pkt_status.set_buzzer)
				sendbuf.pkt_status.buzzer_val = ser_readhex16();

			sendbuf.pkt_status.set_leds = 0;
			sendbuf.pkt_status.leds_val = 0;
			for (int i = 0; i < 4; i++) {
				int v = ser_readtri();
				if (v != -1) {
					sendbuf.pkt_status.set_leds |= 1<<i;
					sendbuf.pkt_status.leds_val |= v<<i;
				}
			}

			sendbuf.pkt_status.eventmask_setbits = ser_readhex();
			sendbuf.pkt_status.eventmask_val = ser_readhex();
			break;
		case 's':
			sendbuf_len += sizeof(sendbuf.pkt_status_ack);

			sendbuf.pkt_status_ack.vm_running = ser_readbool();

			sendbuf.pkt_status_ack.leds = 0;
			for (int i = 0; i < 4; i++) {
				if (ser_readbool())
					sendbuf.pkt_status_ack.leds |= (1 << i);
			}

			sendbuf.pkt_status_ack.buttons = 0;
			for (int i = 0; i < 4; i++) {
				if (ser_readbool())
					sendbuf.pkt_status_ack.buttons |= (1 << i);
			}

			sendbuf.pkt_status_ack.ip = ser_readhex16();
			sendbuf.pkt_status_ack.buzzer = ser_readhex16();
			sendbuf.pkt_status_ack.rgb[0] = ser_readhex();
			sendbuf.pkt_status_ack.rgb[1] = ser_readhex();
			sendbuf.pkt_status_ack.rgb[2] = ser_readhex();
			sendbuf.pkt_status_ack.eventmask = ser_readhex();
			break;
		case 'W':
		case 'w':
		case 'R':
		case 'r':
			/* TBD */
			break;
		case 'X':
		case 'x':
			break;
		}

		/* we should not have seen an end of line yet */
		if (ser_goteol)
			goto parser_error;

		/* there should not be any additional bytes until eol */
		while (cmd != '\r' && cmd != '\n') {
			cmd = ser_readbyte();
			if (cmd == '\r' || cmd == '\n')
				break;
			if (cmd != ' ' && cmd != '\t')
				goto parser_error;
		}

		/* send the pkt */
		ser_endecho();
		net_send();

		break;

	default:
		ser_readeol();
		Serial.println("* Unkown cmd in input!");
		break;

	case '\r':
	case '\n':
	case '*':
		ser_readeol();
		break;
	}

	if (0) {
parser_error:
		ser_readeol();
		Serial.println("* Parser error in input!");
	}

	ser_readeol();
}

/************************************************************
 *                             VM                           *
 ************************************************************/

void vm_reset(void)
{
	vm_running = false;
	vm_stop_next = false;
	vm.sp = vm.sfp = sizeof(vm_mem);

	// testing the demo program
	vm.ip = sizeof(vm_mem);
	vm_running = true;
}

int16_t vm_mem_read(uint16_t addr, bool is16bit, void *ctx)
{
	if (addr < sizeof(vm_mem))
	{
		if (is16bit && addr+1 == sizeof(vm_mem))
			return (vm_mem[addr] << 8) | pgm_read_byte(&(vm_rom[0])); // i wonder if this will ever happen
		if (is16bit)
			return (vm_mem[addr] << 8) | vm_mem[addr+1];
		return vm_mem[addr];
	}
	if (addr + is16bit ? 1 : 0 < sizeof(vm_mem) + sizeof(vm_rom)) {
		if (is16bit)
			return pgm_read_word(&(vm_rom[addr]));
		else
			return pgm_read_byte(&(vm_rom[addr]));
	}
	return 0;
}

void vm_mem_write(uint16_t addr, int16_t value, bool is16bit, void *ctx)
{
	if (addr + (is16bit ? 1 : 0) >= sizeof(vm_mem))
		return; // can't write to rom
	if (is16bit) {
		vm_mem[addr] = value >> 8;
		vm_mem[addr+1] = value;
	} else
		vm_mem[addr] = value;
}

int16_t vm_call_user(uint8_t funcid, uint8_t argc, int16_t *argv, void *ctx)
{
	switch (funcid) {
	case 0:
		vm_running = false;
		return 0;
	case 1: // get leds / set leds / set leds masked
		if (argc >= 1) {
			for (int i=0; i<4; ++i)
				if (argc == 1 || argv[1] & (1<<i)) led(i, argv[1] & (1<<i));
		}
		return (getled(3)<<3) | (getled(2)<<2) | (getled(1)<<1) | getled(0);
	case 2: // get rgb / set rgb
		if (argc < 1) return 0;
		if (argv[0] < 0 || argv[0] > 2) return 0;
		if (argc > 1)
			rgb(argv[0], argv[1]);
		return getrgb(argv[0]);
	case 3: // get buzzer / set buzzer
		if (argc >= 1) buzzer(argv[0]); // i doubt we get meaningful results if we set the buzzer to more than 32kHz, ignoring signedness
		return getbuzzer();
	case 4: // get buttons
		return (button(3)<<3) | (button(2)<<2) | (button(1)<<1) | button(0);
	case 5: // user event
		sendbuf.hdr.pkttype = 'E';
		sendbuf.hdr.seqnum = ++last_send_seq;
		memcpy(&sendbuf.hdr.dst, &base_addr, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_event);

		sendbuf.pkt_event.event_type = ET_USER;
		sendbuf.pkt_event.event_payload = argc ? argv[0] : 0; // we should consider sending 2 words

		net_send_until_acked('e');
	}
}

/************************************************************
 *                      Event handling                      *
 ************************************************************/

void evt_poll()
{
	uint8_t current_buttons = 0; // format as in evt_button_last
	uint8_t button_event = 0;

	for (int i=0; i<4; ++i)
		current_buttons |= button(i) << i;

	button_event |= current_buttons & ~evt_button_last; // newly pressed = down
	button_event |= (~current_buttons & evt_button_last) << 4; // newly pressed = up

	button_event &= evt_button_mask;
	evt_button_last = current_buttons;

	if (!button_event)
		return;

	sendbuf.hdr.pkttype = 'E';
	sendbuf.hdr.seqnum = ++last_send_seq;
	memcpy(&sendbuf.hdr.dst, &base_addr, 8);
	memcpy(&sendbuf.hdr.src, &my_addr, 8);
	sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_event);

	sendbuf.pkt_event.event_type = ET_BUTTON;
	sendbuf.pkt_event.event_payload = button_event;

	net_send_until_acked('e');

#if 0
	// button presses are random events...
	randomSeed(random(0xffff) ^ millis());
#endif
}


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
	}

	if (vm_running) {
		embedvm_exec(&vm);
		if (vm_stop_next == true) {
			vm_running = false;
			vm_stop_next = false;
		}
	}
}

