package org.metalab.ygor.serial.packet;

import java.io.IOException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.serial.BaseStation;

public class BuzzerAdapter implements BuzzerListener {
  private BaseStation base;
  
  public BuzzerAdapter() {
    base = YgorDaemon.baseStation();
  }

  public void loginEvent(Packet pkt) {}
  public void userEvent(Packet pkt) {}
  
  public void transmit(Packet pkt) {
    try {
      base.transmit(pkt);
    } catch (IOException e) {
      throw new YgorException("Unable to transmit packet", e);
    }
  }
  
}
