#ifndef HARDWARE_H
#define HARDWARE_H

#include "OneWire.h"
#include <WProgram.h>

extern OneWire ds;

extern boolean button(byte n);
extern void led(byte n, boolean state);
extern boolean getled(byte n);
extern void rgb(byte r, byte g, byte b);
extern byte getrgb(byte n);
extern void buzzer(uint16_t freq);
extern uint16_t getbuzzer();
extern void hw_setup();

#endif
