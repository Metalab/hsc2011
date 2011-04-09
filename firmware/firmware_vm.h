#ifndef FIRMWARE_VM_H
#define FIRMWARE_VM_H

#include <avr/pgmspace.h>
#include "embedvm.h"

extern bool vm_running;
extern bool vm_stop_next;
extern struct embedvm_s vm;

void vm_reset();
void vm_step(bool allow_send);
int16_t vm_mem_read(uint16_t addr, bool is16bit, void *ctx);
void vm_mem_write(uint16_t addr, int16_t value, bool is16bit, void *ctx);
int16_t vm_call_user(uint8_t funcid, uint8_t argc, int16_t *argv, void *ctx);

#endif
