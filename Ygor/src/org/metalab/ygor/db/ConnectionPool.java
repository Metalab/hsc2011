package org.metalab.ygor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;

public class ConnectionPool extends Service {
  private HashSet<Connection> allConnections;
  private Stack<Connection> availableConnections;
  private Semaphore pool;    
  private String dbUrl;
  private int maxConnections;
  
  protected ConnectionPool(YgorConfig config) {
    super(config);
  }
  
  public void doBoot() throws YgorException {
    this.dbUrl = getYgorConfig().s(YgorConfig.DB_URL);
    this.maxConnections = getYgorConfig().i(YgorConfig.DB_MAX_CONNECTIONS);
    this.allConnections = new HashSet<Connection>();
    this.availableConnections = new Stack<Connection>();
    
    this.pool = new Semaphore(maxConnections, true);
    try {
      Connection conn;
      for (int i = 0; i < maxConnections; i++) {
        conn = createConnection();
        allConnections.add(conn);
        availableConnections.push(conn);
      }
    } catch (SQLException e) {
      throw new YgorException("Unable to preallocate connections", e);
    }
  }

  public void doHalt() throws YgorException {
    pool.drainPermits();
    Iterator<Connection> connIt = allConnections.iterator();
    
    while(connIt.hasNext()) {
      try { connIt.next().close(); } catch (SQLException e) {}
    }

    /*
     *  copy the pool into local scope before closing to prevent 
     *  other threads from accessing the pool while releasing
     */
    Semaphore localSemaCopy = pool;
    close();
    while(localSemaCopy.hasQueuedThreads())
      localSemaCopy.release();
  }
  
  private Connection createConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(dbUrl);
    conn.setAutoCommit(false);
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
    
    return availableConnections.pop();
  }

  public void release(Connection conn) {
    if(isClosed() || availableConnections.contains(conn))
      return;
    
    try {
      conn.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      availableConnections.push(createConnection());
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    pool.release();
  }
}
