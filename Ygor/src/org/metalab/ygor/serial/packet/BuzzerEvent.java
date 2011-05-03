package org.metalab.ygor.serial.packet;

import java.util.EventObject;

import org.metalab.ygor.serial.packet.Packet.PacketType;

public class BuzzerEvent extends EventObject {
  public BuzzerEvent(Packet source) {
    super(source);
  }
  
  public Packet getPacket() {
    return (Packet)getSource();
  }
  
  public PacketType getType() {
    return getPacket().p_type;
  }
}
