package org.metalab.ygor.serial;

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
import org.metalab.ygor.serial.packet.Packet.PacketType;
import org.metalab.ygor.serial.event.PacketListener;
import org.metalab.ygor.serial.event.PacketAdapter;

public class Incoming extends Service {
  private Receive receive;
  private Vector<PacketListener> buzzerListeners = new Vector<PacketListener>();
  
  public Incoming(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    addPacketListener(new LoginListener());
    addPacketListener(new PutDeviceEventListener());
    addPacketListener(new AckListener());
    
    receive = new Receive();
    receive.start();
  }

  public void doHalt() throws YgorException {
    if(receive != null)
      receive.interrupt();
    
    receive = null;
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
      PacketListener l;
      while (itListeners.hasNext()) {
        l = itListeners.next();
        l.packetEvent(pkt);
        switch (pkt.p_type) {
        case PKTT_LOGIN:
          l.loginEvent(pkt);          
          break;

        case PKTT_EVENT:
          l.deviceEvent(pkt);
          break;
          
        case PKTT_STATUS_ACK:
          l.statusAckEvent(pkt);
          break;

        case PKTT_READ_ACK:
          l.readAckEvent(pkt);
          break;

        case PKTT_WRITE_ACK:
          l.writeAckEvent(pkt);
          break;

        case PKTT_VMSTATUS_ACK:
          l.vmStatusAckEvent(pkt);
          break;

        
        default:
          error("Packet type not implemented: " + pkt.p_type);
          break;
        }
      }
    } catch (Exception e) {
      throw new YgorException("Dispatching failed", e);
    }
  }

  private class Receive extends Thread {
    public Receive() {
      super("serial receive");
    }
    
    public void run() {
      Packet pkt;
      while (Incoming.this.isRunning()) {
        try {
          pkt = YgorDaemon.baseStation().receive();
          if (pkt != null)
            dispatch(pkt);
        } catch (Exception e) {
          if(YgorDaemon.baseStation().isRunning())
            warn("receive error", e);
          else {
            error("BaseStation terminated", e);
            YgorDaemon.shutdown();
            break;
          }
        }
      }
    }
  }

  public static class LoginListener extends PacketAdapter {
    private static YgorQuery rmLogin = YgorDaemon.db().createPreparedQuery("login_rm.sql");
    private static YgorQuery attemptLogin = YgorDaemon.db().createPreparedQuery("login_attempt.sql");
    private static YgorQuery ackLogin = YgorDaemon.db().createPreparedQuery("login_ack.sql");
    
    public void loginEvent(Packet loginPkt) {
      Transaction tnx = rmLogin.open(loginPkt);
      attemptLogin.open(tnx, loginPkt);
      ackLogin.open(tnx, loginPkt);

      transmit(loginPkt.createResponse(PacketType.PKTT_LOGIN_ACK, null));

      rmLogin.reset();
      attemptLogin.reset();
      ackLogin.reset();
      tnx.end();
    }
  }

  public static class PutDeviceEventListener extends PacketAdapter {
    private static YgorQuery putIncoming = YgorDaemon.db().createPreparedQuery("incoming_put.sql");

    public void deviceEvent(Packet pkt) {
      pkt.handle = "empty";
      Transaction tnx = putIncoming.open(pkt);
      YgorDaemon.baseStation().getDispatcher().debug("DEVICE CHANGE DETECTED: " + pkt);
      putIncoming.reset();
      tnx.end();
    }
  }
  
  public static class AckListener extends PacketAdapter {
    private static YgorQuery get_outgoing = YgorDaemon.db().createPreparedQuery("outgoing_get.sql");
    private static YgorQuery del_outgoing = YgorDaemon.db().createPreparedQuery("outgoing_del.sql");
    private static YgorQuery putIncoming = YgorDaemon.db().createPreparedQuery("incoming_put.sql");
    
    public void packetEvent(Packet ack) {
      PacketType outgoingType = null;
      switch (ack.p_type) {
      case PKTT_STATUS_ACK:
        outgoingType = PacketType.PKTT_STATUS;
        break;

      case PKTT_READ_ACK:
        outgoingType = PacketType.PKTT_READ;
        break;

      case PKTT_WRITE_ACK:
        outgoingType = PacketType.PKTT_WRITE;
        break;

      case PKTT_VMSTATUS_ACK:
        outgoingType = PacketType.PKTT_VMSTATUS;
        break;
       default:
         return;
      }
      
      Packet initiator = ack.createResponse(outgoingType, null);
      Transaction tnx = get_outgoing.open(initiator);
      YgorResult result = get_outgoing.result();

      if (result.next()) {
        ack.handle = result.getString("handle");
        putIncoming.open(tnx, ack);
        del_outgoing.open(tnx, initiator);
      }

      putIncoming.reset();
      del_outgoing.reset();
      get_outgoing.reset();
      tnx.end();
    }
  }
}
