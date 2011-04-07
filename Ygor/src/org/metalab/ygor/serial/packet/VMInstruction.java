package org.metalab.ygor.serial.packet;

public class VMInstruction extends Payload {
  boolean start;
  boolean stop;
  
  boolean set_ip;
  short[] ip = null;
  
  boolean set_rgb;
  short[] rgb = null;
  
  boolean set_buzzer;
  short[] buzzer = null;

  TriState led0;
  TriState led1;
  TriState led2;
  TriState led3;
  
  short eventmask;
  
  public VMInstruction(String s) {
    
  }
}