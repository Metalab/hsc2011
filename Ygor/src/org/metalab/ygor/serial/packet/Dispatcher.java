package org.metalab.ygor.serial.packet;

import java.util.Iterator;
import java.util.Vector;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.serial.packet.Packet.PacketType;
import org.metalab.ygor.serial.packet.Payload.TriState;

public class Dispatcher extends Service {
  private Receive receive;
  private Vector<BuzzerListener> buzzerListeners = new Vector<BuzzerListener>();
  
  public Dispatcher(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    addBuzzerListener(new LoginListener());
    addBuzzerListener(new UserEventListener());
    
    receive = new Receive();
    receive.start();
  }

  public void doHalt() throws YgorException {
    if(receive != null)
      receive.interrupt();
    
    receive = null;
  }

  public void addBuzzerListener(BuzzerListener l) {
    buzzerListeners.add(l);
  }
  
  public void removeBuzzerListener(BuzzerListener l) {
    buzzerListeners.remove(l);
  }
  
  public void dispatch(Packet pkt) {
    try {
      Iterator<BuzzerListener> itListeners = buzzerListeners.iterator();
      while (itListeners.hasNext()) {
        switch (pkt.p_type) {
        case PKTT_LOGIN:
          itListeners.next().loginEvent(pkt);
          break;

        case PKTT_EVENT:
          itListeners.next().userEvent(pkt);
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
      while (Dispatcher.this.isRunning()) {
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

  public static class LoginListener extends BuzzerAdapter {
    private static YgorQuery attemptLogin = YgorDaemon.db().createPreparedQuery("login_attempt.sql");
    private static YgorQuery ackLogin = YgorDaemon.db().createPreparedQuery("login_ack.sql");
    private static Status enableAllButtons = new Status((short[]) null, -1, TriState.keep, TriState.keep,
        TriState.keep, TriState.keep, (short) 255, (short) 255);
    
    public void loginEvent(Packet loginPkt)  {
      try {
        Transaction tnx = attemptLogin.execute(loginPkt);
        transmit(loginPkt.createResponse(PacketType.PKTT_LOGIN_ACK, null));
        transmit(loginPkt.createResponse(PacketType.PKTT_STATUS,enableAllButtons));
        ackLogin.execute(tnx, loginPkt);
      } finally {
        attemptLogin.close();
        ackLogin.close();
      }
    }
  }

  public static class UserEventListener extends BuzzerAdapter {
    private static YgorQuery logEvent = YgorDaemon.db().createPreparedQuery("event_log.sql");

    public void userEvent(Packet pkt) {
      try {
        logEvent.execute(pkt);
        transmit(pkt.createResponse(PacketType.PKTT_EVENT_ACK, null));
      } finally {
        logEvent.close();
      }
    }
  }
}
