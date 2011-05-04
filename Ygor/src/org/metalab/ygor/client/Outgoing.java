package org.metalab.ygor.client;

import java.util.Iterator;
import java.util.Vector;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorResult;
import org.metalab.ygor.serial.packet.Packet;
import org.metalab.ygor.serial.event.PacketListener;
import org.metalab.ygor.serial.event.PacketAdapter;

public class Outgoing extends Service {
  private Vector<PacketListener> packetListeners = new Vector<PacketListener>();
  private Thread resend;

  public Outgoing(YgorConfig config) {
    super(config);
  }

  public void doBoot() throws YgorException {
    this.resend = new Thread(new Resend());
    this.resend.start();

    addPacketListener(new PutOutgoingListener());
  }

  public void doHalt() throws YgorException {
    if (resend != null)
      resend.interrupt();

    resend = null;
  }

  public void addPacketListener(PacketListener l) {
    synchronized (packetListeners) {
      packetListeners.add(l);
    }
  }

  public void removePacketListener(PacketListener l) {
    synchronized (packetListeners) {
      packetListeners.remove(l);
    }
  }

  public void removeAllPacketListeners() {
    synchronized (packetListeners) {
      packetListeners.clear();
    }
  }

  public void dispatch(Packet pkt) {
    try {
      Iterator<PacketListener> itListeners = packetListeners.iterator();
      while (itListeners.hasNext()) {
        itListeners.next().packetEvent(pkt);
      }
    } catch (Exception e) {
      throw new YgorException("Dispatching failed", e);
    }
  }

  public static class PutOutgoingListener extends PacketAdapter {
    private static YgorQuery putOutgoing = YgorDaemon.db().createPreparedQuery(
        "outgoing_put.sql");

    public void packetEvent(Packet pkt) {
      Transaction tnx = putOutgoing.open(pkt);
      putOutgoing.reset();
      tnx.end();
    }
  }

  private static class Resend extends PacketAdapter implements Runnable {
    private static YgorQuery frontOutgoing = YgorDaemon.db()
        .createPreparedQuery("outgoing_front.sql");

    public void run() {
      Outgoing outgoing = YgorDaemon.client().getOutgoing();
      while (outgoing.isRunning()) {
        Transaction tnx = frontOutgoing.open();
        YgorResult result = frontOutgoing.result();

        if (result.next())
          transmit(Packet.createFromResult(result));

        frontOutgoing.reset();
        tnx.end();

        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
