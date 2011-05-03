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
import org.metalab.ygor.util.ScriptLoader;

public class YgorDB extends Service {
  private ScriptLoader scriptLoader;
  private TransactionPool tnxPool;
  private HashMap<String, NamedQuery> namedQueries = new HashMap<String, NamedQuery>();

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
    if (conf.b(YgorConfig.DB_ALLOW_CREATE)) {
      File dbFile = new File(
          getYgorConfig().s(YgorConfig.DB_URL).split("[:]")[2]);
      if (dbFile.exists())
        dbFile.delete();
    }

    this.tnxPool = new TransactionPool(conf);
    this.tnxPool.doBoot();

    if (conf.b(YgorConfig.DB_ALLOW_CREATE))
      this.bootstrap();

    this.scriptLoader = new ScriptLoader(conf);
    this.scriptLoader.doBoot();
  }

  public void bootstrap() throws YgorException {
    File schemaDir = getYgorConfig().f(YgorConfig.DB_SCHEMA);
    Transaction tnx = tnxPool.acquire("Bootstrap");

    try {
      Connection conn = tnx.getConnection();
      NamedQuery[] sqlListing = NamedQuery.listSqlFiles(schemaDir);

      for (int i = 0; i < sqlListing.length; i++) {
        conn.createStatement().execute(sqlListing[i].getQueryString());
      }
    } catch (Exception e) {
      abortTransaction(tnx);
      throw new YgorException("Unable to bootstreap db schema", e);
    }

    this.endTransaction(tnx);
  }

  public void doHalt() throws YgorException {
    try {
      if (scriptLoader != null)
        scriptLoader.doHalt();
    } catch (Exception e) {
      warn("Unable to halt script loader", e);
    }

    try {
      if (tnxPool != null)
        tnxPool.doHalt();
    } catch (Exception e) {
      warn("Unable to halt transaction ppol", e);
    }
  }

  public YgorQuery createPreparedQuery(String queryName) {
    NamedQuery namedQuery = namedQueries.get(queryName);
    if (namedQuery == null) {
      namedQueries.put(queryName,
          namedQuery = scriptLoader.getNamedQuery(queryName));
    }

    if (namedQuery == null)
      throw new YgorException("Unknown query: " + queryName);
    else {
      return new YgorQuery(namedQuery);
    }
  }

  protected Transaction beginTransaction(String caller) {
    return tnxPool.acquire(caller);
  }

  protected void endTransaction(Transaction tnx) {
    tnxPool.commit(tnx);
  }

  protected void abortTransaction(Transaction tnx) {
    tnxPool.abort(tnx);
  }
}
