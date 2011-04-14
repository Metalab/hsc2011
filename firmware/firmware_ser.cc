/************************************************************
 *                        Serial I/O                        *
 ************************************************************/

#include <avr/eeprom.h>
#include "pktspec.h"
#include "firmware.h"

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

		ser_printhex16(pkt->pkt_status_ack.buzzer);
		Serial.write(' ');

		for (int i=0; i < 3; i++)
			ser_printhex(pkt->pkt_status_ack.rgb[i]);
		Serial.write(' ');

		ser_printhex16(pkt->pkt_status_ack.eventmask);
		break;

	case 'V':
		Serial.write(pkt->pkt_vmstatus.set_running ? (pkt->pkt_vmstatus.running ? 'y' : 'n') : 'z');
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.singlestep ? 'y' : 'n');
		Serial.write(' ');
		Serial.write(pkt->pkt_vmstatus.reset ? 'y' : 'n');
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.set_stacksize ? 'y' : 'n');
		if (pkt->pkt_vmstatus.set_stacksize)
			ser_printhex16(pkt->pkt_vmstatus.stacksize);
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.set_interrupt ? 'y' : 'n');
		Serial.write(pkt->pkt_vmstatus.set_ip ? 'y' : 'n');
		if (pkt->pkt_vmstatus.set_interrupt || pkt->pkt_vmstatus.set_ip)
			ser_printhex16(pkt->pkt_vmstatus.ip);
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.set_sp ? 'y' : 'n');
		if (pkt->pkt_vmstatus.set_sp)
			ser_printhex16(pkt->pkt_vmstatus.sp);
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.set_sfp ? 'y' : 'n');
		if (pkt->pkt_vmstatus.set_sfp)
			ser_printhex16(pkt->pkt_vmstatus.sfp);
		Serial.write(' ');

		Serial.write(pkt->pkt_vmstatus.clear_error ? 'y' : 'n');
		Serial.write(' ');
		Serial.write(pkt->pkt_vmstatus.clear_suspend ? 'y' : 'n');

		break;

	case 'v':
		Serial.write(pkt->pkt_vmstatus_ack.running ? 'y' : 'n');
		Serial.write(' ');
		Serial.write(pkt->pkt_vmstatus_ack.singlestep ? 'y' : 'n');
		Serial.write(' ');
		Serial.write(pkt->pkt_vmstatus_ack.suspended ? 'y' : 'n');
		Serial.write(' ');
		ser_printhex(pkt->pkt_vmstatus_ack.error);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_vmstatus_ack.stacksize);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_vmstatus_ack.ip);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_vmstatus_ack.sp);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_vmstatus_ack.sfp);
		break;

	case 'W':
		ser_printhex(pkt->pkt_write.length);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_write.addr);
		for (int i=0; i<pkt->pkt_write.length; ++i)
			ser_printhex(pkt->pkt_write.data[i]);
		break;
	case 'w':
		break;
	case 'R':
		ser_printhex(pkt->pkt_read.length);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_read.addr);
		break;
	case 'r':
		// this is actually a copy of 'W' with s/pkt_write/pkt_read_ack/g
		ser_printhex(pkt->pkt_read_ack.length);
		Serial.write(' ');
		ser_printhex16(pkt->pkt_read_ack.addr);
		for (int i=0; i<pkt->pkt_read_ack.length; ++i)
			ser_printhex(pkt->pkt_read_ack.data[i]);
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

			sendbuf.pkt_status_ack.buzzer = ser_readhex16();
			sendbuf.pkt_status_ack.rgb[0] = ser_readhex();
			sendbuf.pkt_status_ack.rgb[1] = ser_readhex();
			sendbuf.pkt_status_ack.rgb[2] = ser_readhex();
			sendbuf.pkt_status_ack.eventmask = ser_readhex();
			break;
		case 'V':
			{
				int r = ser_readtri();
				sendbuf.pkt_vmstatus.set_running = r != -1;
				sendbuf.pkt_vmstatus.running = r;
			}

			sendbuf.pkt_vmstatus.singlestep = ser_readbool();

			sendbuf.pkt_vmstatus.reset = ser_readbool();

			sendbuf.pkt_vmstatus.set_stacksize = ser_readbool();
			if (sendbuf.pkt_vmstatus.set_stacksize)
				sendbuf.pkt_vmstatus.stacksize = ser_readhex16();

			sendbuf.pkt_vmstatus.set_interrupt = ser_readbool();
			sendbuf.pkt_vmstatus.set_ip = ser_readbool();
			if (sendbuf.pkt_vmstatus.set_interrupt || sendbuf.pkt_vmstatus.set_ip)
				sendbuf.pkt_vmstatus.ip = ser_readhex16();

			sendbuf.pkt_vmstatus.set_sp = ser_readbool();
			if (sendbuf.pkt_vmstatus.set_sp)
				sendbuf.pkt_vmstatus.sp = ser_readhex16();

			sendbuf.pkt_vmstatus.set_sfp = ser_readbool();
			if (sendbuf.pkt_vmstatus.set_sfp)
				sendbuf.pkt_vmstatus.sfp = ser_readhex16();

			sendbuf.pkt_vmstatus.clear_error = ser_readbool();
			sendbuf.pkt_vmstatus.clear_suspend = ser_readbool();
			break;
		case 'v':
			sendbuf.pkt_vmstatus_ack.running = ser_readbool();
			sendbuf.pkt_vmstatus_ack.singlestep = ser_readbool();
			sendbuf.pkt_vmstatus_ack.suspended = ser_readbool();
			sendbuf.pkt_vmstatus_ack.error = ser_readhex();
			sendbuf.pkt_vmstatus_ack.ip = ser_readhex16();
			sendbuf.pkt_vmstatus_ack.sp = ser_readhex16();
			sendbuf.pkt_vmstatus_ack.sfp = ser_readhex16();
			break;
		case 'W':
			sendbuf.pkt_write.length = ser_readhex();
			if (sendbuf.pkt_write.length > sizeof(sendbuf.pkt_write.data) / sizeof(*sendbuf.pkt_write.data))
				goto parser_error;
			sendbuf.pkt_write.addr = ser_readhex16();
			for(int i = 0; i < sendbuf.pkt_write.length; ++i)
				sendbuf.pkt_write.data[i] = ser_readhex();
			sendbuf_len += sizeof(sendbuf.pkt_write) - 32 + sendbuf.pkt_write.length;
			break;
		case 'w':
			break;
		case 'R':
			sendbuf.pkt_read.length = ser_readhex();
			if (sendbuf.pkt_read.length > sizeof(sendbuf.pkt_read_ack.data) / sizeof(*sendbuf.pkt_read_ack.data))
				goto parser_error;
			sendbuf.pkt_read.addr = ser_readhex16();
			sendbuf_len += sizeof(sendbuf.pkt_read);
			break;
		case 'r':
			// this is actually a copy of 'W' with s/pkt_write/pkt_read_ack/g
			sendbuf.pkt_read_ack.length = ser_readhex();
			if (sendbuf.pkt_read_ack.length > sizeof(sendbuf.pkt_read_ack.data) / sizeof(*sendbuf.pkt_read_ack.data))
				goto parser_error;
			sendbuf.pkt_read_ack.addr = ser_readhex16();
			for(int i = 0; i < sendbuf.pkt_read_ack.length; ++i)
				sendbuf.pkt_read_ack.data[i] = ser_readhex16();
			sendbuf_len += sizeof(sendbuf.pkt_read_ack) - 32 + sendbuf.pkt_read_ack.length;
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
