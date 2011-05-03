package org.metalab.ygor.db;

import java.sql.Connection;
import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;

public class TransactionPool extends Service {
  private long txncounter = 0;
  private ConnectionPool connPool;
  
  public TransactionPool(YgorConfig config) {
    super(config);
  }

  public synchronized void doBoot() throws YgorException {
    connPool = new ConnectionPool(getYgorConfig());
    connPool.boot();
  }

  public synchronized void doHalt() throws YgorException {
    connPool.halt();
  }
  
  private long transactionCount() {
    if (txncounter >= Long.MAX_VALUE)
      txncounter = 0;

    return txncounter++;
  }

  public Transaction acquire(String caller) throws YgorException {
    debug("acquire start: " + caller);

    Connection conn = connPool.acquire();
    Transaction tnx;

    try {
      tnx = new Transaction(conn, caller, transactionCount());
    } catch (Exception e) {
      connPool.release(conn);
      throw new YgorException("Unable to create Transaction", e);
    }

    debug("acquire end: " + tnx);
    return tnx;
  }

  public void commit(Transaction tnx) throws YgorException {
    debug("commit: " + tnx);
    try {
      tnx.commit();
    } finally{
      connPool.release(tnx.getConnection());
    }
  }

  public void abort(Transaction tnx) throws YgorException {
    debug("abort: " + tnx);
    try {
      tnx.rollback();
    } finally {
      connPool.release(tnx.getConnection());
    }
  }
}
