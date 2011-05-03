package org.metalab.ygor.serial.packet;

import java.sql.SQLException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorResult;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.serial.packet.Packet.PacketType;
import org.metalab.ygor.serial.packet.Payload.TriState;
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
        YgorDaemon.baseStation().transmit(pkt);
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

//  public YgorQuery lsPending() throws SQLException {
//    return this.executeAndClose(Q.lsPending, null);
//  }
//  
  public void logEvent(ParameterMap pm) throws SQLException {
    this.executeAndClose(Q.logEvent, pm);
  } 

  public void attemptLogin(ParameterMap pm) throws SQLException {
    this.executeAndClose(Q.loginAttempt, pm);
  }

//  public YgorQuery acceptLogin(ParameterMap pm) throws SQLException {
//    return this.executeAndClose(Q.acceptLogin, pm);
//  }
//
//  
//  public YgorQuery ackVMStatus(ParameterMap pm) throws SQLException {
//    return this.executeAndClose(Q.ackVMStatus, pm);
//  }
//  
//  public YgorQuery queueVMStatus(ParameterMap pm) throws SQLException {
//    return this.executeAndClose(Q.queueVMStatus, pm);
//  }
  
  private void executeAndClose(YgorQuery query, ParameterMap pm) throws YgorException {
    query.execute(this.getClass().toString(), null ,pm);
    query.close();
  }
  
  private static class Q {
    private static YgorQuery acceptLogin = YgorDaemon.db().createPreparedQuery("accept_login.sql");
    private static YgorQuery loginAttempt = YgorDaemon.db().createPreparedQuery("attempt_login.sql");
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
  
  private class Resend extends Thread {
    private long interval;
    private YgorQuery ackLogin = YgorDaemon.db().createPreparedQuery("ack_login.sql");
    private YgorQuery lsAccepted = YgorDaemon.db().createPreparedQuery("ls_accepted_login.sql");
    private Status enableAllButtons = new Status((short[]) null, -1, TriState.keep, TriState.keep,
        TriState.keep, TriState.keep, (short) 255, (short) 255);
    private String callerID = "Resend";
    public Resend(long interval) {
      super("serial resend");
      this.interval = interval;
    }
    
    public void run() {
      while(Dispatcher.this.isRunning()) {
        try {
            Transaction tnx = lsAccepted.execute(callerID);
            YgorResult result = lsAccepted.getResult();

            while (result != null && result.next()) {
              Packet login = Packet.createFromYgorResult(PacketType.PKTT_LOGIN,result);
              Packet ack = login.createResponse(PacketType.PKTT_LOGIN_ACK, null);
              Packet enable = login.createResponse(PacketType.PKTT_STATUS,enableAllButtons);
              dispatch(ack);
              ackLogin.execute(callerID, tnx, login);
              dispatch(enable);
            }
            sleep(interval);
        } catch (Exception e) {
          error("Resending failed", e);
        } finally {
            lsAccepted.close();
        }
      }
    }
  }
}
