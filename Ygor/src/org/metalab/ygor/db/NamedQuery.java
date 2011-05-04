package org.metalab.ygor.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.metalab.ygor.YgorException;

public class NamedQuery {
  private final static FileFilter sqlFilter = new FileFilter(){
    public boolean accept(File f) {
      String name = f.getName();

      if (name.toUpperCase().endsWith(".SQL"))
        return true;
      else
        return false;
    }
  };
 
  private File file;
  private long modtime;
  private String filename;
  private String query;
  private String[] parameters = null;
  private PreparedStatement pstmnt = null;
  private YgorResult result;
  
  public NamedQuery(File f) throws IOException{
    this.file = f;
    this.modtime = f.lastModified();
    this.filename = f.getName();
    this.query = readQueryString(f);
  }
 
  public long getMTime() {
    return modtime;
  }
  
  public File file() {
    return file;
  }
  
  public String name() {
    return filename;
  }
  
  public String getQueryString() {
    return query;
  }
  
  public YgorResult getResult() {
    return result;
  }

  public void reset() {
    try {
      if(result != null)
        result.close();
    } finally {
      pstmnt = null;
      result = null;
    }
  }
  
  public void addBatch() {
    try {
      if(pstmnt != null)
        pstmnt.addBatch();
    } catch (SQLException e) {
      throw new YgorException("Unable to add batch", e);
    }
  }
  public void execute(Transaction tnx, HashMap<String, Object> parameterMap) throws YgorException, SQLException {
      this.pstmnt = tnx.prepareStatement(this.query);

    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        Object p= parameterMap.get(parameters[i]);
        String param = null;
        if(p != null)
          param = p.toString();
        pstmnt.setString(i + 1, param);
      }
    }
      
    try {
      boolean isResultSet = pstmnt.execute();
      if(isResultSet)
        this.result = new YgorResult(pstmnt.getResultSet());
      else 
        this.result = new YgorResult(pstmnt.getUpdateCount());
    } catch (Exception e) {
      throw new YgorException("Unable to execute query", e);
    }
  }
  
  private String readQueryString(File f) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(f));
    StringBuilder sb = new StringBuilder();
    String line;
    char nl = '\n';
    
    if ((line = reader.readLine()) != null && (line = line.trim()).startsWith("--")) {
      this.parameters = (line = line.substring(2)).split("[,]");
      for (int i = 0; i < parameters.length; i++)
        parameters[i] = parameters[i].trim();
    } else
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
          char[] name1 = f1.name().toCharArray();
          char[] name2 = f2.name().toCharArray();
          
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