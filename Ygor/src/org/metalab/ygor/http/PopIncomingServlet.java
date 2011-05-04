package org.metalab.ygor.http;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.db.YgorResult;
import org.metalab.ygor.util.Json;
import org.metalab.ygor.util.ParameterMap;

public class PopIncomingServlet extends YgorServlet {
  private static YgorQuery getIncoming = YgorDaemon.db().createPreparedQuery("incoming_ls.sql");
  private static YgorQuery delIncoming = YgorDaemon.db().createPreparedQuery("incoming_del.sql");

  public PopIncomingServlet() {
    super("text/html");
  }
 
  protected void process(YgorRequest request, OutputStream out)
      throws YgorException {
    
    Vector<String> rowIds = new Vector<String>();
    Transaction tnx = getIncoming.open(getCallerID());
    YgorResult result = getIncoming.result();

    PrintStream ps = new PrintStream(out);
    ps.print(Json.openArray);
    boolean first = true;
    while (result.next()) {
      if (first)
        first = false;
      else {
        ps.print(Json.delimObj);
      }
      rowIds.add(result.getString("rowid"));
      Json.writeRow(result, ps);
    }
    ps.print(Json.closeArray);

    getIncoming.reset();
    tnx.end();

    Iterator<String> itRowId = rowIds.iterator();
    RowIDParam param = new RowIDParam();
    while(itRowId.hasNext()) {
      param.setRowID(itRowId.next());
      tnx = delIncoming.open(getCallerID(), param);
      delIncoming.reset();
      tnx.end();
    }
  }
  
  private class RowIDParam extends HashMap<String, Object> implements ParameterMap {
    public void setRowID(String rowid) {
      this.put("rowid", rowid);
    }

    public HashMap<String, Object> getParameterMap() {
      return this;
    }
  }
}