package org.metalab.ygor.db;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;

import org.metalab.ygor.util.ParameterMap;

public class YgorQuery {
  private final static char openArray = '[';
  private final static char closeArray = ']';
  private final static char openObj = '{';
  private final static char closeObj = '}';
  private final static char tick = '"';
  private final static String delimVal = "\":\"";
  private final static char delimObj = ',';

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
  
  private void writeNextRow(PrintStream out) throws SQLException {
    YgorResult result = getResult();
    String[] columnNames = result.columNames();

    out.print(openObj);
    for (int i = 0; i < columnNames.length; i++) {
      out.print(tick);
      out.print(columnNames[i]);
      out.print(delimVal);
      out.print(result.getString(columnNames[i]));
      out.print(tick);
      if (i < columnNames.length - 1)
        out.print(delimObj);
    }
    out.print(closeObj);
  }

  public void writeJson(PrintStream out) throws SQLException {
    YgorResult result = getResult();
    out.print(openArray);
    boolean first = true;
    while(result.next()) {
      if(first)
        first = false;
      else
        out.print(delimObj);  
      writeNextRow(out);
    }
    out.print(closeArray);
  }
  
  public void writeJson(OutputStream out) throws SQLException {
    writeJson(new PrintStream(out));
  }
}
