package org.metalab.ygor.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import org.metalab.ygor.db.YgorResult;

public class Json {
  public final static char openArray = '[';
  public final static char closeArray = ']';
  public final static char openObj = '{';
  public final static char closeObj = '}';
  public final static char tick = '"';
  public final static String delimVal = "\":\"";
  public final static char delimObj = ',';
  
  public static void writeRow(YgorResult result, PrintStream out) throws SQLException {
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

  public static void writeResult(YgorResult result, PrintStream out) throws SQLException {
    out.print(openArray);
    boolean first = true;
    while(result.next()) {
      if(first)
        first = false;
      else
        out.print(delimObj);  
      writeRow(result, out);
    }
    out.print(closeArray);
  }
  
  public static void writeResult(YgorResult result, OutputStream out) throws SQLException {
    writeResult(result, new PrintStream(out));
  }
}
