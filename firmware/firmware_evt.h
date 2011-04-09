#ifndef FIRMWARE_EVT_H
#define FIRMWARE_EVT_H

#include <stdint.h>

extern uint8_t evt_button_mask; // [ MSB keyup3, .., keyup0, keydown3, .., keydown0 LSB ]
extern uint8_t evt_button_last; // bit number i is true if button i was last held down

void evt_poll();

#endif
