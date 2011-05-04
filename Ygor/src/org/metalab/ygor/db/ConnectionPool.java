package org.metalab.ygor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;

public class ConnectionPool extends Service {
  private Semaphore pool;    
  private String dbUrl;
  private int maxConnections;
  
  protected ConnectionPool(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    this.dbUrl = getYgorConfig().s(YgorConfig.DB_URL);
    this.maxConnections = getYgorConfig().i(YgorConfig.DB_MAX_CONNECTIONS);    
    this.pool = new Semaphore(maxConnections, true);
  }

  public void doHalt() throws YgorException {
    pool.drainPermits();
    /*
     *  copy the pool into local scope before closing to prevent 
     *  other threads from accessing the pool while releasing
     */
    Semaphore localSemaCopy = pool;
    close();

    while(localSemaCopy.hasQueuedThreads())
      localSemaCopy.release();
  }
  
  private Connection createConnection() {
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(dbUrl);
      conn.setAutoCommit(false);
    } catch (SQLException e) {
      error("Unable to create db connection", e);
    }
    assert conn != null;
    return conn;
  }
  
  protected boolean isClosed() {
    return pool == null;
  }

  protected void close() {
    pool = null;
  }

  public Connection acquire() {
    if(isClosed())
      return null;
    
    try {
      pool.acquire();
    } catch (InterruptedException e) {
      error("ACQUIRE INTERRUPTED!!!", e);
    }

    return createConnection();
  }

  public synchronized void release(Connection conn) {
    if (isClosed())
      return;

    try {
      if (!conn.isClosed())
        conn.close();
      else
        return;
    } catch (SQLException e) {
      error("Unable to close db connection", e);
    }

    pool.drainPermits();
    pool.release();
  }
}
