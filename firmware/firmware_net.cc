/************************************************************
 *                          Network                         *
 ************************************************************/

#include "firmware.h"
#include "hardware.h"
#include "RF12.h"

// resend interval in ms from the first attempt to send; chosen arbitrarily
int16_t net_resend_delays[] = { 20, 80, 200, 300, 400, 500, 500 };

uint8_t sendbuf_len;
struct pktbuffer_s sendbuf;
struct pktbuffer_s recvbuf;

bool basestation;
uint8_t ib_addr[8];
uint8_t my_addr[8];
uint8_t base_addr[8];
uint8_t last_send_seq;
uint16_t last_recv_seq; // if we set it to the 8 bit in the package, we'd need an additional binary to know if we are freshly reset
uint16_t last_ping;

bool net_really_process() {
	/* guardian function to avoid duplicate execution
	 *
	 * enclose every capital letter command a buzzer needs to process in an
	 * `if (net_really_process()) { ... }` block, then send an ack. if you
	 * need data from the enclosed block in the ack (as opposed to state
	 * you can get later as well), store it somewhere other than the send
	 * buffer -- the send buffer might be overwritten in between, e.g. by a
	 * button event.
	 * */
	if (last_recv_seq == recvbuf.hdr.seqnum) return false;
	last_recv_seq = recvbuf.hdr.seqnum;
	return true;
}

void net_proc()
{
	ser_printpkt(&recvbuf);

	switch (recvbuf.hdr.pkttype)
	{
	case 'S':
		if (net_really_process())
		{
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
		}

		sendbuf.hdr.pkttype = 's';
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_status_ack);

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
		goto send_ack;
	case 'V':
		if (net_really_process())
		{
			if (recvbuf.pkt_vmstatus.reset) vm_reset();

			if (recvbuf.pkt_vmstatus.set_running)
				vm_running = recvbuf.pkt_vmstatus.running;

			if (recvbuf.pkt_vmstatus.set_singlestep)
				vm_singlestep = recvbuf.pkt_vmstatus.singlestep;

			if (recvbuf.pkt_vmstatus.set_stacksize)
				vm_stack_size = recvbuf.pkt_vmstatus.stacksize;

			if (recvbuf.pkt_vmstatus.set_interrupt)
				embedvm_interrupt(&vm, recvbuf.pkt_vmstatus.ip);

			if (recvbuf.pkt_vmstatus.set_ip)
				vm.ip = recvbuf.pkt_vmstatus.ip;

			if (recvbuf.pkt_vmstatus.set_sp)
				vm.ip = recvbuf.pkt_vmstatus.sp;

			if (recvbuf.pkt_vmstatus.set_sfp)
				vm.ip = recvbuf.pkt_vmstatus.sfp;

			if (recvbuf.pkt_vmstatus.clear_error)
				vm_error = VM_E_NONE;

			if (recvbuf.pkt_vmstatus.clear_suspend)
				vm_suspend = false;
		}

		sendbuf.hdr.pkttype = 'v';
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_vmstatus_ack);

		sendbuf.pkt_vmstatus_ack.running = vm_running;
		sendbuf.pkt_vmstatus_ack.singlestep = vm_singlestep;
		sendbuf.pkt_vmstatus_ack.error = vm_error;
		sendbuf.pkt_vmstatus_ack.stacksize = vm_stack_size;
		sendbuf.pkt_vmstatus_ack.ip = vm.ip;
		sendbuf.pkt_vmstatus_ack.sp = vm.sp;
		sendbuf.pkt_vmstatus_ack.sfp = vm.sfp;

		goto send_ack;
	case 'E':
		// immediately ack -- no processing further than
		// reporting to serial is required
		sendbuf.hdr.pkttype = 'e';
		sendbuf_len = sizeof(struct pktbuffer_hdr_s);

		// send only once -- buzzer will re-transmit the same
		// event, we'll ack it then, and it's up to the
		// software side to know that it was a retransmit.
		goto send_ack;
	case 'W':
		if (net_really_process()) {
			for (uint16_t i=0; i < recvbuf.pkt_write.length; ++i)
				vm_mem_write(recvbuf.pkt_write.addr + i, recvbuf.pkt_write.data[i], false, NULL);
		}

		sendbuf.hdr.pkttype = 'w';
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_write_ack);

		goto send_ack;
	case 'R':
		sendbuf.hdr.pkttype = 'r';
		sendbuf.pkt_read_ack.length = recvbuf.pkt_read.length;
		sendbuf.pkt_read_ack.addr = recvbuf.pkt_read.addr;
		for (uint16_t i=0; i < sendbuf.pkt_read_ack.length; ++i)
			sendbuf.pkt_read_ack.data[i] = vm_mem_read(sendbuf.pkt_read_ack.addr + i, false, NULL);
		sendbuf_len = sizeof(struct pktbuffer_hdr_s) + sizeof(sendbuf.pkt_read_ack) - 32 + sendbuf.pkt_read_ack.length;

		goto send_ack;
	case 'X':
		// ack reset
		do_soft_reset = true;
		sendbuf.hdr.pkttype = 'x';
		sendbuf_len = sizeof(struct pktbuffer_hdr_s);

send_ack:
		sendbuf.hdr.seqnum = recvbuf.hdr.seqnum;
		memcpy(&sendbuf.hdr.dst, &recvbuf.hdr.src, 8);
		memcpy(&sendbuf.hdr.src, &my_addr, 8);
		net_send();
		break;
	case 's':
        case 'v':
	case 'w':
	case 'r':
	case 'L':
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

		vm_step(false);
	}
}

void net_reset() {
	last_recv_seq = 0xffff;
}
