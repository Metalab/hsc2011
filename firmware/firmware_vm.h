#ifndef FIRMWARE_VM_H
#define FIRMWARE_VM_H

#include "embedvm.h"

#define VMMEM_QUICKHW_SIZE 16
#define VMMEM_RAM_SIZE 256
#define VMMEM_EEPROM_SIZE 256
#define VMMEM_FLASH_SIZE 256

#define VMMEM_QUICKHW_START 0
#define VMMEM_QUICKHW_END (VMMEM_QUICKHW_START + VMMEM_QUICKHW_SIZE)
#define VMMEM_RAM_START VMMEM_QUICKHW_END
#define VMMEM_RAM_END (VMMEM_RAM_START + VMMEM_RAM_SIZE)
#define VMMEM_STACK_SIZE VMMEM_RAM_SIZE
#define VMMEM_STACK_START VMMEM_RAM_END
#define VMMEM_STACK_END (VMMEM_STACK_START + VMMEM_STACK_SIZE)
#define VMMEM_EEPROM_START VMMEM_STACK_END
#define VMMEM_EEPROM_END (VMMEM_EEPROM_START + VMMEM_EEPROM_SIZE)
#define VMMEM_FLASH_START VMMEM_EEPROM_END
#define VMMEM_FLASH_END (VMMEM_FLASH_START + VMMEM_FLASH_SIZE)

#define VMMEM_LED0 (VMMEM_QUICKHW_START + 0)
#define VMMEM_LED1 (VMMEM_QUICKHW_START + 1)
#define VMMEM_LED2 (VMMEM_QUICKHW_START + 2)
#define VMMEM_LED3 (VMMEM_QUICKHW_START + 3)
#define VMMEM_LEDS (VMMEM_QUICKHW_START + 4)
#define VMMEM_RGB_R (VMMEM_QUICKHW_START + 5)
#define VMMEM_RGB_G (VMMEM_QUICKHW_START + 6)
#define VMMEM_RGB_B (VMMEM_QUICKHW_START + 7)
#define VMMEM_BUTTON0 (VMMEM_QUICKHW_START + 8)
#define VMMEM_BUTTON1 (VMMEM_QUICKHW_START + 9)
#define VMMEM_BUTTON2 (VMMEM_QUICKHW_START + 10)
#define VMMEM_BUTTON3 (VMMEM_QUICKHW_START + 11)
#define VMMEM_BUTTONS (VMMEM_QUICKHW_START + 12)
#define VMMEM_BUZZER (VMMEM_QUICKHW_START + 13) // only 16it value
// 1 byte left

#define VM_IP_STOP 0xffff

enum vm_error_e
{
	VM_E_NONE,
	VM_E_STACKOVERFLOW, // access to stack ram mapping below -stack_size
	VM_E_STACKACCESS, // access to ram ram mapping above -stack_size
	VM_E_UNMAPPED, // access to unmapped memory or across memory boundaries
	VM_E_RO,// write access to read only memory
};

extern bool vm_running;
extern bool vm_stop_next;
extern struct embedvm_s vm;
extern vm_error_e vm_error;

void vm_reset();
void vm_step(bool allow_send);
int16_t vm_mem_read(uint16_t addr, bool is16bit, void *ctx);
void vm_mem_write(uint16_t addr, int16_t value, bool is16bit, void *ctx);
int16_t vm_call_user(uint8_t funcid, uint8_t argc, int16_t *argv, void *ctx);

#define VM_PHYSICAL_EEPROM_START 16 // address / base station address are at 0 and 8 at length 8 each. we could, alternatively, define this like we did with vm_rom -- then we would have the avr toolchain manage the memory for us. otoh, keeping it like this makes it easier to keep configured addresses across flash cycles

#endif
