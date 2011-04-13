/************************************************************
 *                             VM                           *
 ************************************************************/

#include <avr/pgmspace.h>
#include <avr/eeprom.h>

#include "firmware.h"
#include "hardware.h"

#define UNUSED __attribute__((unused))

bool vm_running;
bool vm_stop_next;
struct embedvm_s vm = { };
uint8_t vm_mem[VMMEM_RAM_SIZE] = { };
uint16_t vm_stack_size;
vm_error_e vm_error;
bool vm_suspend;
unsigned long vm_resumetime;
uint8_t vm_rom[VMMEM_FLASH_SIZE] PROGMEM = {
#include "vmcode/flash.rom"
};
uint8_t vm_trampoline[VMMEM_TRAMPOLINE_SIZE] PROGMEM = {
#include "vmcode/flash.trampoline"
};

void vm_reset(void)
{
	vm_running = false;
	vm_stop_next = false;
	vm.sp = vm.sfp = VMMEM_STACK_END;
	vm_stack_size = VMMEM_RAM_SIZE / 2;
	vm_error = VM_E_NONE;
	vm_suspend = false;

	vm.ip = VM_IP_STOP;
}

void vm_step(bool allow_send)
{
	if (!vm_running) return;
	if (vm_error != VM_E_NONE) return;
	if (vm.ip == VM_IP_STOP) return;
	if (vm_suspend) {
		if ((int16_t)(vm_resumetime - millis()) > 0)
			return;
		else
			vm_suspend = false;
	}

	if (!allow_send && vm_mem_read(vm.ip, false, 0) == 0xb5) return; /* TBD: there might be a better / generalized way to do this */

	embedvm_exec(&vm);
	if (vm_stop_next == true) {
		vm_running = false;
		vm_stop_next = false;
	}
}

int16_t vm_mem_read(uint16_t addr, bool is16bit, void *ctx UNUSED)
{
	if (VMMEM_QUICKHW_START <= addr && addr + is16bit < VMMEM_QUICKHW_END)
	{
		// 16bit flag is ignored here. this will first break when
		// either someone tries to be clever writing bytecode or we
		// have a compiler that converts a (led[1] == 1 && led[2] == 0)
		// to get16(LED1), push_immediate(3), or(), push_immediate(1),
		// jumpifneq() -- which will probably never happen for this vm
		switch(addr)
		{
		case VMMEM_LED0: return getled(0);
		case VMMEM_LED1: return getled(1);
		case VMMEM_LED2: return getled(2);
		case VMMEM_LED3: return getled(3);
		case VMMEM_LEDS: return (getled(3)<<3) | (getled(2)<<2) | (getled(1)<<1) | getled(0);
		case VMMEM_RGB_R: return getrgb(0);
		case VMMEM_RGB_G: return getrgb(1);
		case VMMEM_RGB_B: return getrgb(2);
		case VMMEM_BUTTON0: return button(0);
		case VMMEM_BUTTON1: return button(1);
		case VMMEM_BUTTON2: return button(2);
		case VMMEM_BUTTON3: return button(3);
		case VMMEM_BUTTONS: return (button(3)<<3) | (button(2)<<2) | (button(1)<<1) | button(0);
		case VMMEM_BUZZER: return getbuzzer();
		}
	}
	if (VMMEM_RAM_START <= addr && addr + is16bit < VMMEM_RAM_END)
	{
		if (addr + is16bit >= VMMEM_RAM_END - vm_stack_size) {
			vm_error = VM_E_STACKACCESS;
			return 0;
		}
		if (is16bit)
			return (vm_mem[addr - VMMEM_RAM_START] << 8) | vm_mem[addr - VMMEM_RAM_START + 1];
		return vm_mem[addr - VMMEM_RAM_START];
	}
	if (VMMEM_STACK_START <= addr && addr + is16bit < VMMEM_STACK_END)
	{
		if (addr + is16bit < VMMEM_STACK_END - vm_stack_size) {
			vm_error = VM_E_STACKOVERFLOW;
			return 0;
		}
		if (is16bit)
			return (vm_mem[addr - VMMEM_STACK_START] << 8) | vm_mem[addr - VMMEM_STACK_START + 1];
		return vm_mem[addr - VMMEM_STACK_START];
	}
	if (VMMEM_EEPROM_START <= addr && addr + is16bit < VMMEM_EEPROM_END)
	{
		if (is16bit)
			return (eeprom_read_byte((uint8_t*)(VM_PHYSICAL_EEPROM_START - VMMEM_EEPROM_START + addr))<<8) | eeprom_read_byte((uint8_t*)(VM_PHYSICAL_EEPROM_START - VMMEM_EEPROM_START + addr + 1));
		else
			return eeprom_read_byte((uint8_t*)(VM_PHYSICAL_EEPROM_START - VMMEM_EEPROM_START + addr));
	}
        if (VMMEM_TRAMPOLINE_START <= addr && addr + is16bit < VMMEM_TRAMPOLINE_END) {
		if (is16bit)
			return (pgm_read_byte(&(vm_trampoline[addr-VMMEM_TRAMPOLINE_START]))<<8) | pgm_read_byte(&(vm_trampoline[addr-VMMEM_TRAMPOLINE_START+1]));
		else
			return pgm_read_byte(&(vm_trampoline[addr-VMMEM_TRAMPOLINE_START]));
        }
	if (VMMEM_FLASH_START <= addr && addr + is16bit < VMMEM_FLASH_END)
	{
		if (is16bit)
			return (pgm_read_byte(&(vm_rom[addr-VMMEM_FLASH_START]))<<8) | pgm_read_byte(&(vm_rom[addr-VMMEM_FLASH_START+1]));
		else
			return pgm_read_byte(&(vm_rom[addr-VMMEM_FLASH_START]));
	}
        Serial.print("* Reading unmapped address ");
        Serial.print(addr, HEX);
        Serial.println("");
	vm_error = VM_E_UNMAPPED;
	return 0;
}

void vm_mem_write(uint16_t addr, int16_t value, bool is16bit, void *ctx UNUSED)
{
	if (VMMEM_QUICKHW_START <= addr && addr + is16bit < VMMEM_QUICKHW_END)
	{
		switch(addr)
		{
		case VMMEM_LED0: led(0, value); return;
		case VMMEM_LED1: led(1, value); return;
		case VMMEM_LED2: led(2, value); return;
		case VMMEM_LED3: led(3, value); return;
		case VMMEM_LEDS:
			led(3, value & (1<<3));
			led(2, value & (1<<2));
			led(1, value & (1<<1));
			led(0, value & (1<<0));
			return;
		case VMMEM_RGB_R: rgb(0, value); return;
		case VMMEM_RGB_G: rgb(1, value); return;
		case VMMEM_RGB_B: rgb(2, value); return;
		case VMMEM_BUTTON0:
		case VMMEM_BUTTON1:
		case VMMEM_BUTTON2:
		case VMMEM_BUTTON3:
		case VMMEM_BUTTONS:
			vm_error = VM_E_RO; return;
		case VMMEM_BUZZER: buzzer(value); return;
		}
	}
	if (VMMEM_RAM_START <= addr && addr + is16bit < VMMEM_RAM_END)
	{
		if (addr + is16bit >= VMMEM_RAM_END - vm_stack_size) {
			vm_error = VM_E_STACKACCESS;
			return;
		}
		if (is16bit)
		{
			vm_mem[addr - VMMEM_RAM_START] = value >> 8;
			vm_mem[addr - VMMEM_RAM_START + 1] = value & 0xff;
		} else {
			vm_mem[addr - VMMEM_RAM_START] = value;
		}
		return;
	}
	if (VMMEM_STACK_START <= addr && addr + is16bit < VMMEM_STACK_END)
	{
		if (addr + is16bit < VMMEM_STACK_END - vm_stack_size) {
			vm_error = VM_E_STACKOVERFLOW;
			return;
		}
		if (is16bit)
		{
			vm_mem[addr - VMMEM_STACK_START] = value >> 8;
			vm_mem[addr - VMMEM_STACK_START + 1] = value & 0xff;
		} else {
			vm_mem[addr - VMMEM_STACK_START] = value;
		}
		return;
	}
	if (VMMEM_EEPROM_START <= addr && addr + is16bit < VMMEM_EEPROM_END)
	{
		if (is16bit)
			eeprom_write_word((uint16_t*)(VM_PHYSICAL_EEPROM_START - VMMEM_EEPROM_START + addr), value);
		else
			eeprom_write_byte((uint8_t*)(VM_PHYSICAL_EEPROM_START - VMMEM_EEPROM_START + addr), value);
		return;
	}
	if (VMMEM_FLASH_START <= addr && addr + is16bit < VMMEM_FLASH_END)
	{
		vm_error = VM_E_RO;
		return;
	}
        Serial.print("* Writing to unmapped address ");
        Serial.print(addr, HEX);
        Serial.println("");
	vm_error = VM_E_UNMAPPED;
	return;
}

int16_t vm_call_user(uint8_t funcid, uint8_t argc, int16_t *argv, void *ctx UNUSED)
{
	switch (funcid) {
	case 0: // set an error state
		vm_error = VM_E_USER;
		break;
	case 1: // sleep for a number of milliseconds
		vm_resumetime = millis() + (argc ? argv[0] : 0);
		vm_suspend = true;
		break;
	case 5: // user event -- event id has to match the condition in vm_step concerning allow_send!
		sendbuf.hdr.pkttype = 'E';
		sendbuf.hdr.seqnum = ++last_send_seq;
		memcpy(&sendbuf.hdr.dst, &base_addr, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_event);

		sendbuf.pkt_event.event_type = ET_USER;
		sendbuf.pkt_event.event_payload = argc ? argv[0] : 0; // we should consider sending 2 words

		net_send_until_acked('e');
		break;
	}
}

