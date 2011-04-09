package org.metalab.ygor.serial.packet;

public class VMEvent extends Payload {
  boolean vm_running;
  TriState led0;
  TriState led1;
  TriState led2;
  TriState led3;
  
  boolean button0;
  boolean button1;
  boolean button2;
  boolean button3;
  
  short[] ip;
  short[] buzzer;
  short[] rgb;
  short eventmask;
}

/*
 * ``vm running``: boolean; true if VM is running
 * ``led0`` to ``3``: four booleans indicating which LEDs are on
 * ``button0`` to ``3``: four booleans indicating which buttons are pressed
   (independent of the event mask)
 * ``ip``: current value of the instruction pointer (2 byte hex)
 * ``buzzer``: current vaue of the buzzer in Hz (2 byte hex) or 0 for off
 * ``r``, ``g``, ``b``: hex bytes for red/green/blue part of RGB LED
 * ``eventmask``: as in ``S``
*/