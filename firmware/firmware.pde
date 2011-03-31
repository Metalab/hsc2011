
// Using http://svn.clifford.at/tools/trunk/arduino-cc.sh:
// arduino-cc -P /dev/ttyUSB0 -X 57600 firmware.pde RF12.cpp OneWire.cpp

#include "RF12.h"
#include "OneWire.h"
#include "pktspec.h"

/*
 * Arduino Pin Assignments
 */
const int buttonPins[4] = { 14, 15, 16, 17 };	// a0, a1, a2, a3
const int ledPins[4] = { 18, 8, 4, 7 };		// a4, d8, d4, d7
const int rgbPins[3] = { 3, 5, 6 };		// d3, d5, d6
const int onewirePin = 19;			// a5
const int buzzerPin = 9;			// d9

OneWire ds(onewirePin);	

boolean button(byte n)
{
	return digitalRead(buttonPins[n]) == LOW;
}

void led(byte n, boolean state)
{
	digitalWrite(ledPins[n], state ? HIGH : LOW);
}

void rgb(byte r, byte g, byte b)
{
	analogWrite(rgbPins[0], r);
	analogWrite(rgbPins[1], g);
	analogWrite(rgbPins[2], b);
}

void buzzer(uint16_t freq)
{
#if 0
	// This somhow breaks the PWM on on of the RGB channels
	// so we use the hand written OCR1 setup code below..
	if (freq > 0)
		tone(buzzerPin, freq);
	else
		noTone(buzzerPin);
#else
	uint16_t ocr = freq > 0 ? 0xffff / (freq >> 1) : 0;

	TCCR1A = (freq > 0 ? _BV(COM1A0) : 0);
	TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);
	TCCR1C = 0;

	OCR1A  = ocr;
	OCR1B  = 0;
	ICR1   = 0;
	TIMSK1 = 0;
	TIFR1  = 0;

	if (freq > 0 && TCNT1 > OCR1A) {
		// TCCR1C = _BV(FOC1A);
		TCNT1 = 0;
	}

	// Serial.print("* buzzer freq=");
	// Serial.print(freq, DEC);
	// Serial.print(", ocr=");
	// Serial.print(ocr, DEC);
	// Serial.println("");
#endif
}

void setup()
{
	// initialize serial communication
	Serial.begin(57600);
	Serial.println("* Inititalizing...");

	// initialize RFM12 module
	rf12_initialize(1, RF12_868MHZ);

	// the button pins are inputs with pullups:
	for (int i=0; i<4; i++) {
		pinMode(buttonPins[i], INPUT);
		digitalWrite(buttonPins[i], HIGH);
	}

	// the LEDs are outputs:
	for (int i=0; i<4; i++) {
		pinMode(ledPins[i], OUTPUT);
		digitalWrite(ledPins[i], LOW);
		led(i, 0);
	}

	// the buzzer is an output
	pinMode(buzzerPin, OUTPUT);
	digitalWrite(buzzerPin, LOW);
	buzzer(0);

	// RGB Leds are also outputs
	for (int i=0; i<3; i++) {
		pinMode(rgbPins[i], OUTPUT);
		digitalWrite(rgbPins[i], LOW);
	}
	rgb(0, 0, 0);

	Serial.println("* Init done. Entering main loop.");
}

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

void loop()
{
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

			rgb(r, g, b);
			delay(30);

			if (!busy)
				state++;
		}

		led(0, LOW);
		led(1, LOW);
		led(2, LOW);
		led(3, LOW);

		rgb(0, 0, 0);
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

