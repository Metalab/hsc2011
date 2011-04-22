package org.metalab.ygor.serial.packet;

public class VMStatusResponse extends Payload {
  boolean running;
  boolean singlestep;
  boolean suspended;

  short error; // actually unsigned 8-bit value
  int stacksize;
  short[] ip;
  short[] sp;
  short[] sfp;
}
