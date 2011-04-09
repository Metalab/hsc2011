package org.metalab.ygor.serial.packet;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.serial.packet.Packet.PacketType;

public class Dispatcher extends Service implements Runnable {
  private Thread readLoop;
  
  public Dispatcher(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    readLoop = new Thread(this);
    readLoop.start();
  }

  public void doHalt() throws YgorException {
    if(readLoop != null)
      readLoop.interrupt();
    
    readLoop = null;
  }

  public void dispatch(Packet pkt) {
    try {
      switch (pkt.p_type) {
      case PKTT_LOGIN:
        attemptLogin(pkt.src,pkt.src, pkt.payloadHex, now());
        break;

      case PKTT_LOGIN_ACK:
        YgorDaemon.baseStation().transmit(pkt);
        break;

      default:
        break;
      }
    } catch (Exception e) {
      throw new YgorException("Dispatching failed", e);
    }
  }

  private static Date now() {
    return new Date(System.currentTimeMillis());
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
 
  public YgorQuery attemptLogin(String src, String dest, String payload, Date at) throws SQLException {
    Q.loginAttempt.execute(new Object[] { src, dest, payload, at });
    return Q.loginAttempt;
  } 

  public YgorQuery lsAccepted() throws SQLException {
    Q.lsAccepted.execute();
    return Q.lsPending;
  }

  public YgorQuery lsPending() throws SQLException {
    Q.lsPending.execute();
    return Q.lsPending;
  }
  
  public YgorQuery acceptLogin(String src) throws SQLException {
    Q.acceptLogin.execute(new Object[] { src });
    return Q.acceptLogin;
  }
  
  private Packet createLoginAck(String src) throws SQLException {
    Q.getLogin.execute(new Object[]{src});
    return new Packet(PacketType.PKTT_LOGIN_ACK, (ResultSet)Q.getLogin.getResultSet());
  }
  
  private static class Q {
    private static YgorQuery acceptLogin = YgorDaemon.db().createPreparedQuery("accept_login.sql");
    private static YgorQuery loginAttempt = YgorDaemon.db().createPreparedQuery("attempt_login.sql");
    private static YgorQuery lsAccepted = YgorDaemon.db().createPreparedQuery("ls_accepted_logins.sql");
    private static YgorQuery lsPending = YgorDaemon.db().createPreparedQuery("ls_pending_logins.sql");
    private static YgorQuery getLogin = YgorDaemon.db().createPreparedQuery("get_login.sql");
  }
}
