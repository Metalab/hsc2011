package org.metalab.ygor.client;

import java.util.Iterator;
import java.util.Vector;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.serial.packet.Packet;
import org.metalab.ygor.serial.event.PacketListener;
import org.metalab.ygor.serial.event.PacketAdapter;

public class Outgoing extends Service {
  private Vector<PacketListener> buzzerListeners = new Vector<PacketListener>();
  
  public Outgoing(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    addPacketListener(new PutOutgoingListener());
  }

  public void doHalt() throws YgorException {

  }

  public void addPacketListener(PacketListener l) {
    buzzerListeners.add(l);
  }
  
  public void removePacketListener(PacketListener l) {
    buzzerListeners.remove(l);
  }
  
  public void dispatch(Packet pkt) {
    try {
      Iterator<PacketListener> itListeners = buzzerListeners.iterator();
      while (itListeners.hasNext()) {
        itListeners.next().packetEvent(pkt);
      }
    } catch (Exception e) {
      throw new YgorException("Dispatching failed", e);
    }
  }

  public static class PutOutgoingListener extends PacketAdapter {
    private static YgorQuery putOutgoing = YgorDaemon.db().createPreparedQuery("outgoing_put.sql");
    
    public void packetEvent(Packet pkt)  {
      try {
        putOutgoing.execute(pkt);
        transmit(pkt);
      } finally {
        putOutgoing.close();
      }
    }
  }
}
