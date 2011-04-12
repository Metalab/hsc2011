package org.metalab.ygor.db;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.NamedQuery.ResultSetType;
import org.metalab.ygor.util.ParameterMap;

public class YgorQuery {
  private final static char openArray = '[';
  private final static char closeArray = ']';
  private final static char openObj = '{';
  private final static char closeObj = '}';
  private final static char tick = '"';
  private final static String delimVal = "\" = \"";
  private final static char delimObj = ',';
  
  private int columnCount;
  private String[] headerNames;
  private boolean isOpen = true;
  private NamedQuery namedQuery;
  private Connection connection;
  
  public YgorQuery(Connection connection, NamedQuery namedQuery) throws SQLException {
    this.connection = connection;
    this.namedQuery = namedQuery;
  }

  public void execute() throws YgorException, SQLException {
    this.execute((ParameterMap)null);
  }
  
  public void execute(ParameterMap pm) throws YgorException, SQLException {
    if(pm != null)
      namedQuery.execute(connection, pm.getParameterMap());
    else
      namedQuery.execute(connection, null);
    
    switch (namedQuery.rs_type) {
    case RESULT_SET:
      ResultSet rs = (ResultSet) namedQuery.result;

      ResultSetMetaData rsmd = rs.getMetaData();
      this.columnCount = rsmd.getColumnCount();
      this.headerNames = new String[this.columnCount];

      for (int i = 0; i < headerNames.length; i++) {
        this.headerNames[i] = rsmd.getColumnName(i + 1);
      }
      break;
    case BOOLEAN:
      this.columnCount = 1;
      this.headerNames = new String[] { "Successful" };
      break;
    case INTEGER:
      this.columnCount = 1;
      this.headerNames = new String[] { "Updated" };      
      break;
    default:
      break;
    }
  }
  
  public Object getResult(){
    return namedQuery.result;
  }

  public boolean isOpen() throws SQLException {
    if(isOpen && namedQuery.rs_type == ResultSetType.RESULT_SET)
      return (isOpen = ((ResultSet) namedQuery.result).next());

    return isOpen;
  }
  
  private void writeNextRow(PrintStream out) throws SQLException {
    if (namedQuery.rs_type == ResultSetType.RESULT_SET) {
      ResultSet rs = (ResultSet) namedQuery.result;
      
      out.print(openObj);
      for (int i = 0; i < headerNames.length; i++) {
        out.print(tick);
        out.print(headerNames[i]);
        out.print(delimVal);
        out.print(rs.getString(i + 1));
        out.print(tick);
        if(i < headerNames.length - 1)
          out.print(delimObj);
      }
      out.print(closeObj);
    } else {
      out.print(openObj);
      out.print(tick);
      out.print(headerNames[0]);
      out.print(delimVal);
      out.print(namedQuery.result);
      out.print(tick);
      out.print(closeObj);
      isOpen = false;
    }
  }

  public void writeJson(PrintStream out) throws SQLException {
    out.print(openArray);
    while(isOpen()) {
      writeNextRow(out);
      out.print(delimObj);
    }
    out.print(closeArray);
    
    if(namedQuery.rs_type == ResultSetType.RESULT_SET)
      ((ResultSet) namedQuery.result).close();
  }
  
  public void writeJson(OutputStream out) throws SQLException {
    writeJson(new PrintStream(out));
  }
}
