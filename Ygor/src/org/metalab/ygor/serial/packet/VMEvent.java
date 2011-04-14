// i'd call this StatusResponse.java --chrysn
package org.metalab.ygor.serial.packet;

public class VMEvent extends Payload {
  TriState led0;
  TriState led1;
  TriState led2;
  TriState led3;
  
  boolean button0;
  boolean button1;
  boolean button2;
  boolean button3;
  
  short[] buzzer;
  short[] rgb;
  short eventmask;
}
