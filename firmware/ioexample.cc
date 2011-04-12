
// Using http://svn.clifford.at/tools/trunk/arduino-cc.sh:
// arduino-cc -P /dev/ttyUSB0 -X 57600 ioexample.cc hardware.cc RF12.cpp OneWire.cpp

#include <WProgram.h>

#include "RF12.h"
#include "OneWire.h"
#include "pktspec.h"
#include "hardware.h"

uint16_t last_pwrcheck_millis;

void poll_RF12()
{
	if (rf12_recvDone() && rf12_crc == 0)
	{
		Serial.print("GOT PKT:");
		for (byte i = 0; i < rf12_len; ++i) {
			Serial.print(" ");
			Serial.print(rf12_data[i], DEC);
		}
		Serial.println();

		if (rf12_len == 6) {
			for (int i=0; i<4; i++)
				led(i, rf12_data[i]);
			buzzer(((int)rf12_data[5] << 8) | rf12_data[4]);
		}
	}
}

void setup()
{
	hw_setup();
	last_pwrcheck_millis = millis();
}

void loop()
{
	if (millis() - last_pwrcheck_millis > 1000)
	{
		Serial.print("* Power status: ");
		Serial.println(analogRead(0), DEC);
		last_pwrcheck_millis = millis();
	}

	if (button(0))
	{
		Serial.println("* Running Button 0 demo.");
		for (int i=0; i<4; i++) {
			led(i, 1);
			delay(500);
			led(i, 0);
			delay(500);
		}
		return;
	}

	if (button(1))
	{
		Serial.println("* Running Button 1 demo.");
		for (int i=0; i<4; i++) {
			led(i, 1);
		}
		for (int i=0; i<2000; i+=3) {
			buzzer(i);
			delay(1);
		}
		buzzer(0);
		delay(100);
		for (int i=0; i<4; i++) {
			led(i, 0);
			for (int j=200; j>0; j-=20) {
				buzzer(j);
				delay(5);
			}
			buzzer(0);
			delay(500);
		}
		return;
	}

	if (button(2))
	{
		byte r = 0, g = 0, b = 0, state = 1;

		Serial.println("* Running Button 2 demo.");
		while (state <= 8)
		{
			boolean busy = false;

			if (state & 1) {
				if (r < 255)
					busy = true, r += 5;
			} else {
				if (r > 0)
					busy = true, r -= 5;
			}

			if (state & 2) {
				if (g < 255)
					busy = true, g += 5;
			} else {
				if (g > 0)
					busy = true, g -= 5;
			}

			if (state & 4) {
				if (b < 255)
					busy = true, b += 5;
			} else {
				if (b > 0)
					busy = true, b -= 5;
			}

			led(0, state & 1);
			led(1, state & 2);
			led(2, state & 4);
			led(3, state & 8);

			rgb(0, r);
			rgb(1, g);
			rgb(2, b);
			delay(30);

			if (!busy)
				state++;
		}

		led(0, LOW);
		led(1, LOW);
		led(2, LOW);
		led(3, LOW);

		rgb(0, 0);
		rgb(1, 0);
		rgb(2, 0);
		return;
	}

	if (button(3))
	{
		Serial.println("* Running Button 3 demo.");
		for (int i=1; i<=16; i++)
		{
			led(0, i & 1 ? HIGH : LOW);
			led(1, i & 1 ? HIGH : LOW);
			led(2, i & 1 ? HIGH : LOW);
			led(3, i & 1 ? HIGH : LOW);

			while (!rf12_canSend())
				poll_RF12();

			rf12_len = 6;
			rf12_data[0] = i & 1 ? 1 : 0;
			rf12_data[1] = i & 2 ? 1 : 0;
			rf12_data[2] = i & 4 ? 1 : 0;
			rf12_data[3] = i & 8 ? 1 : 0;
			rf12_data[4] = ((100*(i%16)) >> 0) & 0x00ff;
			rf12_data[5] = ((100*(i%16)) >> 8) & 0x00ff;
			rf12_sendStart(0);
			rf12_sendWait(0);

			delay(300);
		}
		led(0, LOW);
		led(1, LOW);
		led(2, LOW);
		led(3, LOW);
	}

	poll_RF12();

	byte ds_addr[8];
	ds.reset_search();
	if (ds.search(ds_addr) == 1)
	{
		byte xorhash = 0;
		Serial.print("* OneWire Addr:");
		for (int i = 0; i < 8; i++) {
			xorhash = xorhash ^ ds_addr[i];
			Serial.print(" ");
			Serial.print(ds_addr[i], HEX);
		}
		xorhash = (xorhash & 0x0f) ^ (xorhash >> 4);
		Serial.println("");

		led(0, (xorhash & _BV(0)) ? HIGH : LOW);
		led(1, (xorhash & _BV(1)) ? HIGH : LOW);
		led(2, (xorhash & _BV(2)) ? HIGH : LOW);
		led(3, (xorhash & _BV(3)) ? HIGH : LOW);

		for (int i = 0; i <= 5; i++) {
			buzzer((i & 1) == 0 ? 440 : 0);
			delay(100);
		}

		led(0, LOW);
		led(1, LOW);
		led(2, LOW);
		led(3, LOW);
	}
}

