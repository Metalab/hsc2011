/************************************************************
 *                      Event handling                      *
 ************************************************************/

#include "hardware.h"
#include "firmware.h"

uint8_t evt_button_mask; // [ MSB keyup3, .., keyup0, keydown3, .., keydown0 LSB ]
uint8_t evt_button_last; // bit number i is true if button i was last held down

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
