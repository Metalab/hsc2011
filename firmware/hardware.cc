
#include <WProgram.h>

#include "RF12.h"
#include "OneWire.h"
#include "hardware.h"

/*
 * Arduino Pin Assignments
 */
const int buttonPins[4] = { 14, 15, 16, 17 };	// a0, a1, a2, a3
const int ledPins[4] = { 18, 8, 4, 7 };		// a4, d8, d4, d7
const int rgbPins[3] = { 3, 5, 6 };		// d3, d5, d6
const int onewirePin = 19;			// a5
const int buzzerPin = 9;			// d9

// if we run into extreme memory constraints, we can probably receive those
// values from hardware state -- but not with the arduino calls we are
// currently using
int current_rgb[3] = {0, 0, 0};
uint16_t current_buzzer;

OneWire ds(onewirePin);	

boolean button(byte n)
{
	return digitalRead(buttonPins[n]) == LOW;
}

void led(byte n, boolean state)
{
	digitalWrite(ledPins[n], state ? HIGH : LOW);
}

boolean getled(byte n)
{
	digitalRead(ledPins[n]);
}

void rgb(byte n, byte val)
{
	analogWrite(rgbPins[n], val);
	current_rgb[n] = val;
}

byte getrgb(byte n)
{
	return current_rgb[n];
}

void buzzer(uint16_t freq)
{
	current_buzzer = freq;
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

uint16_t getbuzzer()
{
	return current_buzzer;
}

uint16_t getpwr()
{
	uint16_t v = analogRead(0);
	pinMode(buttonPins[0], INPUT);
	digitalWrite(buttonPins[0], HIGH);
	return v;
}

void hw_setup()
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
	}

	// the buzzer is an output
	pinMode(buzzerPin, OUTPUT);
	digitalWrite(buzzerPin, LOW);

	// RGB Leds are also outputs
	for (int i=0; i<3; i++) {
		pinMode(rgbPins[i], OUTPUT);
		digitalWrite(rgbPins[i], LOW);
	}

	Serial.println("* Init done. Entering main loop.");
}


void hw_reset_soft()
{
	for (int i=0; i<4; ++i)
		led(i, 0);
	buzzer(0);
	rgb(0, 0);
	rgb(1, 0);
	rgb(2, 0);
}
