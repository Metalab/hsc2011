package org.metalab.ygor.serial.packet;

import org.metalab.ygor.YgorException;

public class Address {
  public String hex;
  
  public Address(String s) {
    if(s.length() != 16)
      throw new YgorException("Malformed address: " + s);

     this.hex = s;
  }
}
