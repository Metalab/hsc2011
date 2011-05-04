package org.metalab.ygor.db;

import org.metalab.ygor.YgorException;

import org.metalab.ygor.util.ParameterMap;

public class YgorQuery {
  private NamedQuery namedQuery;
  
  public YgorQuery(NamedQuery namedQuery) {
    this.namedQuery = namedQuery;
  }

  public Transaction open() throws YgorException {
    return this.open(getCallerClassName(), null, null);
  }
  public Transaction open(String caller) throws YgorException {
    return this.open(caller, null, null);
  }
  
  public Transaction open(ParameterMap pm) throws YgorException {
    return this.open(getCallerClassName(), null, pm);
  }
  
  public Transaction open(Transaction tnx) throws YgorException {
    return this.open(getCallerClassName(), tnx, null);
  }
  
  public Transaction open(Transaction tnx, ParameterMap pm) throws YgorException {
    return this.open(getCallerClassName(), tnx, pm);
  }
  
  public Transaction open(String caller, ParameterMap pm) throws YgorException {
    return this.open(caller, null, pm);
  }

  public Transaction open(String caller, Transaction tnx) throws YgorException {
    return this.open(caller, tnx, null);
  }
  
  public Transaction open(String caller, Transaction tnx, ParameterMap pm) throws YgorException {
    if(tnx == null)
      tnx = Transaction.create(caller);
    
    try {
      if(pm != null)
        namedQuery.execute(tnx, pm.getParameterMap());
      else
        namedQuery.execute(tnx, null);
    } catch (Exception e) {
      tnx.abort();
      throw new YgorException("YgorQuery failed", e);
    }
    
    return tnx;
  }
  
  public void addBatch() {
    namedQuery.addBatch();
  }
  
  public void reset() {
    namedQuery.reset();    
  }

  public YgorResult result(){
    return namedQuery.getResult();
  }

  private static String getCallerClassName() {
    try {
      throw new Exception();
    } catch (Exception e) {
      return e.getStackTrace()[2].toString();
    }
  }
}
