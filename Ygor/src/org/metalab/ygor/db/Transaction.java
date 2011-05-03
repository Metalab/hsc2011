package org.metalab.ygor.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

import org.metalab.ygor.YgorException;

public class Transaction {
  private String caller;
  private long txnnr;
  private long creationTime;
  private Connection connection;
  private Vector<PreparedStatement> trackedStmnts = new Vector<PreparedStatement>();
  private boolean terminated = false;
  protected Transaction(Connection connection, String caller, long txnnr) throws SQLException{
    this.connection = connection;
    this.creationTime = System.currentTimeMillis();
    this.caller = caller;
    this.txnnr = txnnr;
    this.connection.setAutoCommit(false);
  }

  public long creationTime() {
    return creationTime;
  }

  public long runningTime() {
    return System.currentTimeMillis() - creationTime;
  }

  protected long transactionNumber() {
    return txnnr;
  }

  private void deleteStatements() {
    try {
      Iterator<PreparedStatement> it = trackedStmnts.iterator();
      
      while(it.hasNext())
        it.next().close();

      trackedStmnts.clear();
    } catch (SQLException e) {
      throw new YgorException("Unable to clear statements", e);
    }
  }

  protected void rollback() {
    checkOpen();
    try {
      if(!terminated && !connection.isReadOnly() && !connection.getAutoCommit())
        connection.rollback();
      connection.setAutoCommit(true);
      terminated = true;
      deleteStatements();
    } catch (SQLException e) {
      throw new YgorException("Unable to rollback transaction", e);
    }
  }

  
  protected void commit() {
    checkOpen();
    try {
      try {
        if(!terminated && !connection.isReadOnly() && !connection.getAutoCommit())
          connection.commit();
      } catch (Exception e) {
        e.printStackTrace();
      }
      connection.setAutoCommit(true);
      terminated = true;
      deleteStatements();
    } catch (SQLException e) {
      throw new YgorException("Unable to commit transaction", e);
    }
  }
  
  protected PreparedStatement prepareStatement(String query) {
    checkOpen();
    try {
      PreparedStatement pstmnt = connection.prepareStatement(query);
      trackedStmnts.add(pstmnt);
      return pstmnt;
    } catch (SQLException e) {
      throw new YgorException("Unable to prepare statement: " + query, e);
    }
  }
  
  protected Connection getConnection() {
    return connection;
  }

  public boolean isOpen() {
    try {
      return !connection.isClosed();
    } catch (SQLException e) {
      throw new YgorException("Invalid connection", e);
    }
  }

  public void checkOpen() {
    if (!isOpen())
      throw new YgorException("Unexpected closed connection");
  }

  public String caller() {
    return caller;
  }

  public String toString() {
    return "Transaction(" + this.txnnr + " / " + runningTime() + "): "
        + caller();
  }
}