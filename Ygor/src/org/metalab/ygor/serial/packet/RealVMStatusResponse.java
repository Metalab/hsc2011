// i'd call this VMStatusResponse.java; prefixing with Real to avoid confusion with VMEvent.java --chrysn
package org.metalab.ygor.serial.packet;

public class RealVMStatusResponse extends Payload {
  boolean running;
  boolean singlestep;
  boolean suspended;

  short error; // actually unsigned 8-bit value
  int stacksize;
  short[] ip;
  short[] sp;
  short[] sfp;
}
