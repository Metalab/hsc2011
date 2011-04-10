package org.metalab.ygor.serial.packet;

import org.metalab.ygor.YgorException;

public class IButton extends Payload {
  public String hex;
  
  public IButton(String s) {
    if(s.length() != 16)
      throw new YgorException("Malformed address: " + s);

     this.hex = s;
  }
}
