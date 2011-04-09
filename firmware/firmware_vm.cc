/************************************************************
 *                             VM                           *
 ************************************************************/

#include "firmware.h"
#include "hardware.h"

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

void vm_reset(void)
{
	vm_running = false;
	vm_stop_next = false;
	vm.sp = vm.sfp = sizeof(vm_mem);

	// testing the demo program
	vm.ip = sizeof(vm_mem);
	vm_running = true;
}

void vm_step(bool allow_send)
{
	if (!vm_running) return;
	if (!allow_send && vm_mem_read(vm.ip, false, 0) == 0xb5) return; /* TBD: there might be a better / generalized way to do this */

	embedvm_exec(&vm);
	if (vm_stop_next == true) {
		vm_running = false;
		vm_stop_next = false;
	}
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
			return pgm_read_word(&(vm_rom[addr-sizeof(vm_mem)]));
		else
			return pgm_read_byte(&(vm_rom[addr-sizeof(vm_mem)]));
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
				if (argc == 1 || argv[1] & (1<<i)) led(i, argv[0] & (1<<i));
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

