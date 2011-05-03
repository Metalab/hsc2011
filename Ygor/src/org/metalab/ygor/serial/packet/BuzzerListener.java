package org.metalab.ygor.serial.packet;

import java.util.EventListener;

public interface BuzzerListener extends EventListener {
  public void loginEvent(Packet pkt);
  public void userEvent(Packet pkt);
}
