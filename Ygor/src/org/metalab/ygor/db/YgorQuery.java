package org.metalab.ygor.db;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;

import org.metalab.ygor.util.ParameterMap;

public class YgorQuery {
  private YgorDB db;
  private NamedQuery namedQuery;
  private Transaction currentTnx = null;
  
  public YgorQuery(NamedQuery namedQuery) {
    this.namedQuery = namedQuery;
    this.db = YgorDaemon.db();
  }

  public Transaction execute(String caller) throws YgorException {
    return this.execute(caller, null, null);
  }
  
  public Transaction execute(ParameterMap pm) throws YgorException {
    return this.execute(getCallerClassName(), null, pm);
  }
  
  public Transaction execute(Transaction tnx) throws YgorException {
    return this.execute(getCallerClassName(), tnx, null);
  }
  
  public Transaction execute(Transaction tnx, ParameterMap pm) throws YgorException {
    return this.execute(getCallerClassName(), tnx, pm);
  }
  
  public Transaction execute(String caller, ParameterMap pm) throws YgorException {
    return this.execute(caller, null, pm);
  }

  public Transaction execute(String caller, Transaction tnx) throws YgorException {
    return this.execute(caller, tnx, null);
  }
  
  public Transaction execute(String caller, Transaction tnx, ParameterMap pm) throws YgorException {
    Transaction createdTnx = null;
    if(tnx != null)
      currentTnx = tnx;
    else if(currentTnx == null)
      currentTnx = createdTnx = db.beginTransaction(caller);      
    
    try {
      if(pm != null)
        namedQuery.execute(currentTnx, pm.getParameterMap());
      else
        namedQuery.execute(currentTnx, null);
    } catch (Exception e) {
      db.abortTransaction(currentTnx);
      throw new YgorException("YgorQuery failed", e);
    }
    
    return createdTnx;
  }
  
  public void addBatch() {
    namedQuery.addBatch();
  }
  
  public void close() {
    if(currentTnx != null)
      db.endTransaction(currentTnx);
    
    currentTnx = null;
    namedQuery.reset();
  }

  public YgorResult getResult(){
    return namedQuery.getResult();
  }

  public boolean isOpen() {
    return currentTnx != null;
  }
  
  private static String getCallerClassName() {
    try {
      throw new Exception();
    } catch (Exception e) {
      return e.getStackTrace()[2].toString();
    }
  }
}
