
// Using http://svn.clifford.at/tools/trunk/arduino-cc.sh:
// arduino-cc -P /dev/ttyUSB0 -X 57600 firmware.cc hardware.cc RF12.cpp OneWire.cpp

#include <WProgram.h>

#include "RF12.h"
#include "OneWire.h"
#include "pktspec.h"
#include "hardware.h"

uint8_t my_addr[8];
uint8_t base_addr[8];
uint16_t last_send_seq;
uint16_t last_recv_seq;

uint8_t sendbuf_len;
struct pktbuffer_s sendbuf;
struct pktbuffer_s recvbuf;

void net_proc();
void net_poll();
void net_send();

void net_proc()
{
	/* TBD */
}

void net_poll()
{
	// a new pkt to process?
	if (!rf12_recvDone() || rf12_crc != 0)
		return;

	// is it for us?
	if (memcmp(&my_addr, ((struct pktbuffer_s*)rf12_data)->dst, 8))
		return;

	// is it really new?
	if (((struct pktbuffer_s*)rf12_data)->seqnum == last_recv_seq) {
		net_send();
		return;
	}

	// copy to recv buffer
	memcpy(&recvbuf, (void*)rf12_data, rf12_len);
	last_recv_seq = recvbuf.seqnum;

	// procces
	net_proc();
}

void net_send()
{
	// drop every incoming until we can send
	while (!rf12_canSend())
		rf12_recvDone();

	// copy to RFM12 lib and transmit
	rf12_sendStart(0, &sendbuf, sendbuf_len);

	// wait for ack if needed and resend - TBD
}

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
	if (c == '\n' || c == '\r')
		ser_goteol = true;
	return c;
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

int ser_readmac(uint8_t *buf)
{
	char cmd = ser_readbyte();
	if (cmd == '*') {
		memcpy(buf, my_addr, 8);
	} else if (cmd == '$') {
		memcpy(buf, base_addr, 8);
	} else {
		ser_unget_char = cmd;
		for (int i = 0; i < 8; i++)
			buf[i] = ser_readhex();
	}
}

uint16_t ser_readhex16()
{
	uint16_t val = ser_readhex() << 8;
	val |= ser_readhex();
	return val;
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
		switch(ch) {
			default:
				set_goteol = true;
				/* fall through */
			case ET_BUTTON_PRESS:
			case ET_BUTTON_RELEASE:
			case ET_USER:
				return ch
			case ' ':
			case '\t':
				continue;
		}
	}
}

void ser_poll()
{
	if (!Serial.available())
		return;

	ser_goteol = false;
	ser_unget_char = 0;
	char cmd = ser_readbyte();

	switch (cmd)
	{
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
		/* set pkttype */
		sendbuf.pkttype = cmd;

		/* parse src and dest mac addr */
		ser_readmac(sendbuf.src);
		ser_readmac(sendbuf.dst);

		/* parse seq num */
		sendbuf.seqnum = ser_readhex16();

		/* size of generic pkt header */
		sendbuf_len = (byte*)&sendbuf.dst[7] - (byte*)&sendbuf;
		
		switch (cmd)
		{
		case 'L':
			sendbuf_len += sizeof(sendbuf.pkt_login);
			sendbuf.pkt_login.using_ibutton = ser_readbool();
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
			sendbuf.pkt_status.clear_leds = 0;
			for (int i = 0; i < 4; i++) {
				int v = ser_readtri();
				if (v == 1)
					sendbuf.pkt_status.set_leds |= (1 << i);
				if (v == 0)
					sendbuf.pkt_status.clear_leds |= (1 << i);
			}

			sendbuf.pkt_status.eventmask_val = 0xff;
			sendbuf.pkt_status.eventmask_setbits = 0xff;
			for (int i = 0; i < 8; i++) {
				int v = ser_readtri();
				if (v == 0)
					sendbuf.pkt_status.eventmask_val &= ~(1 << i);
				if (v == -1)
					sendbuf.pkt_status.eventmask_setbits &= ~(1 << i);
			}
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
		net_send();

		break;

	default:
		if (1) {
			Serial.println("* Unkown cmd in input!");
		} else {
parser_error:
			Serial.println("* Parser error in input!");
		}
		/* fall thru */
	case '\r':
	case '\n':
	case '*':
		while (cmd != '\r' && cmd != '\n')
			cmd = ser_readbyte();
		break;
	}
}

void setup()
{
	hw_setup();
}

void loop()
{
	net_poll();
	ser_poll();

	// run_vm();

	// check buttons - TBD

	// read serial cmd
}

