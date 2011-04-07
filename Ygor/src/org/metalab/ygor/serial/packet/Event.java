package org.metalab.ygor.serial.packet;

import org.metalab.ygor.YgorException;

public class Event {
  public Event(String payload) {
    if(payload.length() != 5)
      throw new YgorException("Malformed payload (len != 5): " + payload);
    
    type = payload.charAt(0);
    higher = Short.parseShort(payload.substring(1, 3),16);
    lower = Short.parseShort(payload.substring(3, 5),16);
  }
  
  public char type;
  public short higher;
  public short lower;  
}
