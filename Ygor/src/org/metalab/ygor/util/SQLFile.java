package org.metalab.ygor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class SQLFile {
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

  public SQLFile(File f) throws IOException{
    this.modtime = f.lastModified();
    this.query = readQueryString(f);
    this.name = f.getName();
    this.f = f;
  }

  private String readQueryString(File f) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(f));
    StringBuilder sb = new StringBuilder();
    String line;
    char nl = '\n';

    while ((line = reader.readLine()) != null)
      sb.append(line).append(nl);

    return sb.toString();
  }

  public static SQLFile[] listSqlFiles(File dir) throws IOException {
    if(!dir.isDirectory())
      throw new IOException(dir.getCanonicalPath() + " is not a directory");

    File[] listing = dir.listFiles(sqlFilter);
    
    if (listing.length > 0) {
      SQLFile[] sqlFiles = new SQLFile[listing.length];
      for (int i = 0; i < sqlFiles.length; i++) {
        sqlFiles[i] = new SQLFile(listing[i]);
      }

      Arrays.sort(sqlFiles, new Comparator<SQLFile>() {
        public int compare(SQLFile f1, SQLFile f2) {
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