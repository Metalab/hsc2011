package org.metalab.ygor.serial.packet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.serial.packet.Packet.PacketType;

public class Dispatcher extends Service {
  private Receive receive;
  private Resend resend;
  
  public Dispatcher(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    receive = new Receive();
    receive.start();
    
    resend = new Resend(getYgorConfig().l(YgorConfig.SERIAL_RESEND_INTERVAL));
    resend.start();
  }

  public void doHalt() throws YgorException {
    if(receive != null)
      receive.interrupt();
    
    receive = null;
    
    if(resend != null)
      resend.interrupt();
    
    resend = null;
  }

  public void dispatch(Packet pkt) {
    try {
      switch (pkt.p_type) {
      case PKTT_LOGIN:
        attemptLogin(pkt.createParameterMap());
        break;

      case PKTT_LOGIN_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;

      case PKTT_EVENT:
        logEvent(pkt.createParameterMap());
        dispatch(pkt.createResponse(PacketType.PKTT_EVENT_ACK, null));
        break;
        
      case PKTT_EVENT_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;
        
      case PKTT_STATUS:
        
        break;

      case PKTT_STATUS_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;

      default:
        error("Packet type not implemented: " + pkt.p_type);
        break;
      }
    } catch (Exception e) {
      throw new YgorException("Dispatching failed", e);
    }
  }

  public YgorQuery lsAccepted() throws SQLException {
    Q.lsAccepted.execute();
    return Q.lsPending;
  }

  public YgorQuery lsPending() throws SQLException {
    Q.lsPending.execute();
    return Q.lsPending;
  }
  
  public YgorQuery logEvent(HashMap<String, Object> paramMap) throws SQLException {
    Q.logEvent.execute(paramMap);
    return Q.logEvent;
  } 

  public YgorQuery attemptLogin(HashMap<String, Object> paramMap) throws SQLException {
    Q.loginAttempt.execute(paramMap);
    return Q.loginAttempt;
  }

  public YgorQuery acceptLogin(YgorRequest request ) throws SQLException {
    Q.acceptLogin.execute(request);
    return Q.acceptLogin;
  }
/*
  public YgorQuery ackVMevent(String src, String dest, short seqnum) throws SQLException {
    Q.ackVMEvent.execute(new Object[] { src, dest, seqnum });
    return Q.ackVMEvent;
  }
  
  public YgorQuery queueVMEvent(String src, String dest, short seqnum) throws SQLException {
    Q.queueVMEvent.execute(new Object[] { src, dest, seqnum });
    return Q.queueVMEvent;
  }*/
  
  private static class Q {
    private static YgorQuery acceptLogin = YgorDaemon.db().createPreparedQuery("accept_login.sql");
    private static YgorQuery loginAttempt = YgorDaemon.db().createPreparedQuery("attempt_login.sql");
    private static YgorQuery lsAccepted = YgorDaemon.db().createPreparedQuery("ls_accepted_logins.sql");
    private static YgorQuery lsPending = YgorDaemon.db().createPreparedQuery("ls_pending_logins.sql");
    private static YgorQuery getLogin = YgorDaemon.db().createPreparedQuery("get_login.sql");
    private static YgorQuery logEvent = YgorDaemon.db().createPreparedQuery("log_event.sql");
    private static YgorQuery ackVMEvent = YgorDaemon.db().createPreparedQuery("ack_vmevent.sql");
    private static YgorQuery lsPendingVMEvent = YgorDaemon.db().createPreparedQuery("ls_pending_vmevent.sql");
    private static YgorQuery queueVMEvent = YgorDaemon.db().createPreparedQuery("queue_vmevent.sql");
  }

  private class Receive extends Thread {
    public Receive() {
      super("serial receive");
    }
    
    public void run() {
      Packet pkt;
      while (true) {
        try {
          pkt = YgorDaemon.baseStation().receive();
          if (pkt != null)
            dispatch(pkt);
        } catch (Exception e) {
          warn("receive error", e);
        }
      }
    }
  }
  
  private class Resend extends Thread {
    private long interval;
    public Resend(long interval) {
      super("serial resend");
      this.interval = interval;
    }
    
    public void run() {
//      while(Dispatcher.this.isRunning()) {
//        try {
//          ResultSet rs = (ResultSet) lsPending().getResultSet();
//          while (rs.next()) {
//            dispatch(Packet.createFromResultSet(PacketType.PKTT_LOGIN_ACK, Q.getLogin.getResultSet()));
//          }
//          sleep(interval);
//        } catch (Exception e) {
//          error("Resending failed", e);
//        }
//      }
    }
  }
}
