package org.metalab.ygor.serial.event;

import java.util.EventListener;

import org.metalab.ygor.serial.packet.Packet;

public interface PacketListener extends EventListener {
  public void loginEvent(Packet pkt);
  public void deviceEvent(Packet pkt);
  public void packetEvent(Packet pkt);
  public void statusAckEvent(Packet pkt);
  public void readAckEvent(Packet pkt);
  public void writeAckEvent(Packet pkt);
  public void vmStatusAckEvent(Packet pkt);
}
