package org.metalab.ygor.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.util.SQLFile;
import org.metalab.ygor.util.ScriptLoader;

public class YgorDB extends Service {
  private Connection connection;
  private ScriptLoader scriptLoader;
  private HashMap<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

  public YgorDB(YgorConfig config) {
    super(config);
  }

  public void doBoot() throws YgorException {
    YgorConfig conf = getYgorConfig();
    try {
      Class.forName(conf.s(YgorConfig.DB_DRIVER));
    } catch (ClassNotFoundException e) {
      throw new YgorException("Unable to load JDBC driver", e);
    }

    try {
      this.connection = DriverManager.getConnection(conf.s(YgorConfig.DB_URL));
    } catch (SQLException e) {
      throw new YgorException("Unable to connect to database", e);
    }

    try {
      this.connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new YgorException("DB connection failed", e);
    }
    
    if(conf.b(YgorConfig.DB_ALLOW_CREATE))
      this.bootstrap();
    
    this.scriptLoader = new ScriptLoader(conf);
    this.scriptLoader.doBoot();
  }

  public void bootstrap() throws YgorException {
    File schemaDir = getYgorConfig().f(YgorConfig.DB_SCHEMA);
    File dbFile = new File(getYgorConfig().s(YgorConfig.DB_URL).split("[:]")[2]);
    try {
      dbFile.delete();
    } catch (Exception e) {
      warn("Unable to delete db file: " + dbFile.getName(), e);
    }
    
    String lastQuery = null;
    try {
      SQLFile[] sqlListing = SQLFile.listSqlFiles(schemaDir);
      
      for (int i = 0; i < sqlListing.length; i++) {
        connection.createStatement().execute(lastQuery = sqlListing[i].query);
      }
    } catch (IOException e) {
      throw new YgorException("Unable to access db schema dir: " + schemaDir.getName(), e);
    } catch (SQLException e) {
      throw new YgorException("DB schema query failed: " + lastQuery, e);
    }
  }
  
  public void doHalt() throws YgorException {
    try {
      if(connection != null)
        connection.close();
    } catch (SQLException e) {
      warn("Unable to close connection", e);
    }

    try {
      if(scriptLoader != null)
        scriptLoader.doHalt();
    } catch (Exception e) {
      warn("Unable to halt script loader", e);
    }
  }

  public YgorQuery createQuery(String queryString) throws SQLException {
    if(!getYgorConfig().b(YgorConfig.DEBUG)) {
      throw new YgorException("Won't execute raw queries unless in debug mode: " + queryString);
    } else {
      return new YgorQuery(connection.createStatement(), queryString);
    }
  }
  
  public YgorQuery createPreparedQuery(String queryName)  {
    return createPreparedQuery(queryName, null);
  }
  
  //FIXME dirty query parameter handling
  public YgorQuery createPreparedQuery(String queryName, HashMap<String, String> params)  {
    try {
      PreparedStatement pstmnt = preparedStatements.get(queryName);
      if (pstmnt == null) {
        String queryString = scriptLoader.getNamedQuery(queryName);
        if (queryString != null) {
          pstmnt = connection.prepareStatement(queryString);
          preparedStatements.put(queryName, pstmnt);
        }
      }

      if(pstmnt == null)
        throw new YgorException("Unknown query: " + queryName);
      else {
        if (params != null) {
          String seqParam;
          for (int i = 0; (seqParam = params.get(String.valueOf(i))) != null; i++) {
            pstmnt.setString(i + 1, seqParam);
          }
        }
        return new YgorQuery(pstmnt);
      }
    } catch (SQLException e) {
      throw new YgorException("create prepared query failed", e);
    }
  }
}
