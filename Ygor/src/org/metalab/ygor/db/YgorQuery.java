package org.metalab.ygor.db;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.metalab.ygor.YgorException;

public class YgorQuery {
  private final static char open = '{';
  private final static char close = '}';
  private final static char tick = '"';
  private final static String delimVal = "\" = \"";
  private final static char delimCol = ',';
  
  private enum ResultSetType  { RESULT_SET, BOOLEAN, INTEGER };
  private ResultSetType rs_type;
  private Statement stmnt;
  private String queryString;
  private Object result;
  private int columnCount;
  private String[] headerNames;
  private boolean isOpen = true;

  public YgorQuery(Statement stmnt, String query) throws SQLException {
    if(query == null)
      throw new NullPointerException("Can't execute statement without a query string");
    this.stmnt = stmnt;
    this.queryString = query;
  }
  
  public YgorQuery(PreparedStatement pstmnt) throws SQLException {
    this.stmnt = pstmnt;
    this.queryString = null;
  }
  
  public void execute() throws YgorException, SQLException {
    execute(null);
  }
  
  public ResultSet getResultSet() {
    return (ResultSet)result;
  }
  
  public boolean getBool() {
    return (Boolean)result;
  }
  
  public int getInt() {
    return (Integer)result;
  }
  
  public void execute(Object[] params) throws YgorException, SQLException {
    executeStatment(params);
    
    switch (rs_type) {
    case RESULT_SET:
      ResultSet rs = (ResultSet) this.result;

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
  
  private void executeStatment(Object[] params) throws YgorException, SQLException {
    PreparedStatement pstmnt = null;
    if(queryString == null) {
      pstmnt = ((PreparedStatement) stmnt);
      Class paramClass = null;
      if(params != null) {
        for (int i = 0; i < params.length; i++) {
          paramClass = params[i].getClass();
          if(paramClass == Integer.class)
            pstmnt.setInt(i + 1, (Integer)params[i]);
          else if(paramClass == Long.class)
            pstmnt.setLong(i + 1, (Long)params[i]);
          else if(paramClass == Float.class)
            pstmnt.setFloat(i + 1, (Float)params[i]);
          else if(paramClass == Double.class)
            pstmnt.setDouble(i + 1, (Double)params[i]);
          else if(paramClass == Boolean.class)
            pstmnt.setBoolean(i + 1, (Boolean)params[i]);
          else if(paramClass == Date.class)
            pstmnt.setDate(i + 1, (Date)params[i]);
          else if(paramClass == String.class)
            pstmnt.setString(i + 1, (String)params[i]);
        }
      }
    }
      
    try {
      if(pstmnt != null)
        this.result = pstmnt.executeQuery();
      else
        this.result = stmnt.executeQuery(queryString);
      
      rs_type = ResultSetType.RESULT_SET;
    } catch (SQLException e) {
      try {
        if(pstmnt != null)
          this.result = pstmnt.execute();
        else
          this.result = stmnt.execute(queryString);
        
        rs_type = ResultSetType.BOOLEAN;
      } catch (SQLException e1) {
        try {
          if(pstmnt != null)
            this.result = pstmnt.executeUpdate();
          else
            this.result = stmnt.executeUpdate(queryString);
          
          rs_type = ResultSetType.INTEGER;
        } catch (SQLException e2) {
          throw new YgorException("Unable to execute query", e);
        }
      }
    }
  }

  public boolean isOpen() throws SQLException {
    if(isOpen && rs_type == ResultSetType.RESULT_SET)
      return (isOpen = ((ResultSet) this.result).next());

    return isOpen;
  }
  
  private void writeNextRow(PrintStream out) throws SQLException {
    if (rs_type == ResultSetType.RESULT_SET) {
      ResultSet rs = (ResultSet) this.result;

      out.print(open);
      for (int i = 0; i < headerNames.length; i++) {
        out.print(tick);
        out.print(headerNames[i]);
        out.print(delimVal);
        out.print(rs.getString(i + 1));
        out.print(tick);
        if(i < headerNames.length - 1)
          out.print(delimCol);
      }
      out.print(close);

    } else {
      out.print(open);
      out.print(tick);
      out.print(headerNames[0]);
      out.print(delimVal);
      out.print(this.result);
      out.print(tick);
      out.print(close);
      isOpen = false;
    }
  }

  public void writeJson(PrintStream out) throws SQLException {
    out.println(open);
    while(isOpen())
      writeNextRow(out);
    out.println(close);
    
    if(rs_type == ResultSetType.RESULT_SET)
      ((ResultSet) this.result).close();
  }
  
  public void writeJson(OutputStream out) throws SQLException {
    writeJson(new PrintStream(out));
  }
}
