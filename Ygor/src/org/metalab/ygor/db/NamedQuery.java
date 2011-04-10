package org.metalab.ygor.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.metalab.ygor.YgorException;

public class NamedQuery {
  public enum ResultSetType  { RESULT_SET, BOOLEAN, INTEGER };

  private final static FileFilter sqlFilter = new FileFilter(){
    public boolean accept(File f) {
      String name = f.getName();

      if (name.toUpperCase().endsWith(".SQL"))
        return true;
      else
        return false;
    }
  };
 
  public long modtime;
  public String query;
  public String name;
  public File f;
  public String[] parameters = null;
  private PreparedStatement pstmnt = null;
  public ResultSetType rs_type;
  public Object result;
  
  public NamedQuery(File f) throws IOException{
    this.modtime = f.lastModified();
    this.query = readQueryString(f);
    this.name = f.getName();
    this.f = f;
  }

  private PreparedStatement getPreparedStatement(Connection connection) throws SQLException {
    if(pstmnt == null) {
      pstmnt = connection.prepareStatement(query);
    }
    return pstmnt;
  }
  
  public void execute(Connection connection, HashMap<String, Object> parameterMap) throws YgorException, SQLException {
    PreparedStatement pstmnt = getPreparedStatement(connection);

    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        pstmnt.setString(i + 1, parameterMap.get(parameters[i]).toString());
      }
    }
      
    try {
      this.result = pstmnt.executeQuery();
      rs_type = ResultSetType.RESULT_SET;
    } catch (SQLException e) {
      try {
        this.result = pstmnt.execute();
        rs_type = ResultSetType.BOOLEAN;
      } catch (SQLException e1) {
        try {
          this.result = pstmnt.executeUpdate();
          rs_type = ResultSetType.INTEGER;
        } catch (SQLException e2) {
          throw new YgorException("Unable to execute query", e);
        }
      }
    }
  }
  
  private String readQueryString(File f) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(f));
    StringBuilder sb = new StringBuilder();
    String line;
    char nl = '\n';
    
    if ((line = reader.readLine()) != null && line.trim().startsWith("--"))
      this.parameters = line.split("[,]");
    else
      sb.append(line).append(nl);
    
    while ((line = reader.readLine()) != null)
      sb.append(line).append(nl);

    return sb.toString();
  }

  public static NamedQuery[] listSqlFiles(File dir) throws IOException {
    if(!dir.isDirectory())
      throw new IOException(dir.getCanonicalPath() + " is not a directory");

    File[] listing = dir.listFiles(sqlFilter);
    
    if (listing.length > 0) {
      NamedQuery[] sqlFiles = new NamedQuery[listing.length];
      for (int i = 0; i < sqlFiles.length; i++) {
        sqlFiles[i] = new NamedQuery(listing[i]);
      }

      Arrays.sort(sqlFiles, new Comparator<NamedQuery>() {
        public int compare(NamedQuery f1, NamedQuery f2) {
          char[] name1 = f1.name.toCharArray();
          char[] name2 = f2.name.toCharArray();
          
          int len = Math.min(name1.length, name2.length);
          for (int i = 0; i < len; i++) {
            if(name1[i] != name2[i]) {
              if(name1[i] > name2[i]) 
                return 1;
              else 
                return -1;
            }
          }
          return 0;
        }
      });
      
      return sqlFiles;
    }
    return null;
  }
}