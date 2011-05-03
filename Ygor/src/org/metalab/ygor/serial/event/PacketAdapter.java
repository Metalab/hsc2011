package org.metalab.ygor.serial.event;

import java.io.IOException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.serial.BaseStation;
import org.metalab.ygor.serial.packet.Packet;

public class PacketAdapter implements PacketListener {
  private BaseStation base;
  
  public PacketAdapter() {
    base = YgorDaemon.baseStation();
  }

  public void loginEvent(Packet pkt) {}
  public void deviceEvent(Packet pkt) {}
  public void packetEvent(Packet pkt) {}
  public void statusAckEvent(Packet pkt) {}
  public void readAckEvent(Packet pkt) {}
  public void writeAckEvent(Packet pkt) {}
  public void vmStatusAckEvent(Packet pkt) {}
  
  public void transmit(Packet pkt) {
    try {
      base.transmit(pkt);
    } catch (IOException e) {
      throw new YgorException("Unable to transmit packet", e);
    }
  }
  
}
