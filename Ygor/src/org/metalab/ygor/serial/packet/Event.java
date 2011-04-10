package org.metalab.ygor.serial.packet;

import org.metalab.ygor.YgorException;

public class Event extends Payload {
  public char type;
  public short reserved;
  public short eventmask;  

  public Event(String payload) {
    if(payload.length() != 5)
      throw new YgorException("Malformed payload (len != 5): " + payload);
    
    String tokens[] = payload.split("\\s");
    
    type = tokens[0].charAt(0);
    String eventHex = tokens[1];
    reserved = Short.parseShort(eventHex.substring(0,2));
    eventmask = Short.parseShort(eventHex.substring(2,4));
  }
}
