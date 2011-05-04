package org.metalab.ygor.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;

public class Transaction {
  private String caller;
  private long txnnr;
  private long creationTime;
  private Connection connection;
  
  protected Transaction(Connection connection, String caller, long txnnr) {
    this.connection = connection;
    this.creationTime = System.currentTimeMillis();
    this.caller = caller;
    this.txnnr = txnnr;
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

  protected synchronized void rollback() {
    try {
      if(isOpen() && !connection.isReadOnly() && !connection.getAutoCommit())
        connection.rollback();
    } catch (SQLException e) {
      throw new YgorException("Unable to rollback transaction", e);
    }
  }

  protected synchronized void commit() {
    try {
      if(isOpen() && !connection.isReadOnly() && !connection.getAutoCommit())
        connection.commit();
    } catch (SQLException e) {
      throw new YgorException("Unable to commit transaction", e);
    }
  }
  
  protected PreparedStatement prepareStatement(String query) {
    checkOpen();
    try {
      return connection.prepareStatement(query);
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
  
  protected static Transaction create(String caller) {
      return YgorDaemon.db().beginTransaction(caller);
  }

  public void end() {
    if(this.isOpen())
      YgorDaemon.db().endTransaction(this);
  }

  public void abort() {
    if(this.isOpen())
      YgorDaemon.db().abortTransaction(this);
  }
}