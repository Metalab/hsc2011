package org.metalab.ygor.http;

import java.io.OutputStream;
import java.sql.SQLException;

import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorQuery;
import org.metalab.ygor.db.YgorRequest;

public class QueryServlet extends YgorServlet {
	public QueryServlet() {
		super("text/html");
	}

	protected void process(YgorRequest request, OutputStream out) throws YgorException {
	  YgorQuery query = request.execute("Query servlet");
	  try {
	    query.execute();
      query.writeJson(out);
    } catch (SQLException e) {
      throw new YgorException("SQL query failed", e);
    }
	}
}