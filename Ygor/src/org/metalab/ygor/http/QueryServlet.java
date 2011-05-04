package org.metalab.ygor.http;

import java.io.OutputStream;

import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.Transaction;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.util.Json;

public class QueryServlet extends YgorServlet {
  public QueryServlet() {
    super("text/html");
  }

  protected void process(YgorRequest request, OutputStream out)
      throws YgorException {
    YgorQuery query = request.execute();
    Transaction tnx = query.open(getCallerID(), request);
    Json.writeResult(query.result(), out);
    query.reset();
    tnx.end();
  }
}