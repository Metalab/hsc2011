package org.metalab.ygor.serial.packet;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.serial.packet.Packet.PacketType;
import org.metalab.ygor.util.ParameterMap;

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
        attemptLogin(pkt);
        break;

      case PKTT_LOGIN_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;

      case PKTT_EVENT:
        logEvent(pkt);
        dispatch(pkt.createResponse(PacketType.PKTT_EVENT_ACK, null));
        break;
        
      case PKTT_EVENT_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;
        
      case PKTT_STATUS:
      case PKTT_VMSTATUS:
        break;

      case PKTT_STATUS_ACK:
      case PKTT_VMSTATUS_ACK:
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
    return Q.lsAccepted;
  }

  public YgorQuery lsPending() throws SQLException {
    Q.lsPending.execute();
    return Q.lsPending;
  }
  
  public YgorQuery logEvent(ParameterMap pm) throws SQLException {
    Q.logEvent.execute(pm);
    return Q.logEvent;
  } 

  public YgorQuery attemptLogin(ParameterMap pm) throws SQLException {
    Q.loginAttempt.execute(pm);
    return Q.loginAttempt;
  }

  public YgorQuery acceptLogin(ParameterMap pm) throws SQLException {
    Q.acceptLogin.execute(pm);
    return Q.acceptLogin;
  }

  public YgorQuery ackLogin(ParameterMap pm) throws SQLException {
    Q.ackLogin.execute(pm);
    return Q.ackLogin;
  }
  
  public YgorQuery ackVMStatus(ParameterMap pm) throws SQLException {
    Q.ackVMStatus.execute(pm);
    return Q.ackVMStatus;
  }
  
  public YgorQuery queueVMStatus(ParameterMap pm) throws SQLException {
    Q.queueVMStatus.execute(pm);
    return Q.queueVMStatus;
  }
  
  private static class Q {
    private static YgorQuery acceptLogin = YgorDaemon.db().createPreparedQuery("accept_login.sql");
    private static YgorQuery ackLogin = YgorDaemon.db().createPreparedQuery("ack_login.sql");
    private static YgorQuery loginAttempt = YgorDaemon.db().createPreparedQuery("attempt_login.sql");
    private static YgorQuery lsAccepted = YgorDaemon.db().createPreparedQuery("ls_accepted_login.sql");
    private static YgorQuery lsPending = YgorDaemon.db().createPreparedQuery("ls_pending_login.sql");
    private static YgorQuery getLogin = YgorDaemon.db().createPreparedQuery("get_login.sql");
    private static YgorQuery logEvent = YgorDaemon.db().createPreparedQuery("log_event.sql");
    private static YgorQuery ackVMStatus = YgorDaemon.db().createPreparedQuery("ack_vmstatus.sql");
    private static YgorQuery lsPendingVMStatus = YgorDaemon.db().createPreparedQuery("ls_pending_vmstatus.sql");
    private static YgorQuery queueVMStatus = YgorDaemon.db().createPreparedQuery("queue_vmstatus.sql");
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
      while(Dispatcher.this.isRunning()) {
        try {
          ResultSet rs = (ResultSet) lsAccepted().getResult();
          
          while (rs != null && rs.next()) {
            Packet ack = Packet.createFromResultSet(PacketType.PKTT_LOGIN_ACK, rs);
            dispatch(ack);
            ackLogin(ack);
          }
          sleep(interval);
        } catch (Exception e) {
          error("Resending failed", e);
        }
      }
    }
  }
}
