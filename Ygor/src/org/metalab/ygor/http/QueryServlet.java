package org.metalab.ygor.http;

import java.io.OutputStream;
import java.sql.SQLException;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorDB;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorRequest;

public class QueryServlet extends YgorServlet {
  private YgorDB db;
  
  public QueryServlet() {
    super("text/html");
    db = YgorDaemon.db();
  }
 
  protected void process(YgorRequest request, OutputStream out)
      throws YgorException {
    YgorQuery query = request.execute();
    try {
      query.execute(getCallerID(), request);
      query.writeJson(out);
    } catch (SQLException e) {
      throw new YgorException("SQL query failed", e);
    }

    query.close();
  }
}