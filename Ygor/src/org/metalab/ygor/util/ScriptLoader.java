package org.metalab.ygor.util;

import java.io.*;
import java.util.HashMap;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.NamedQuery;

public class ScriptLoader extends Service {
	private File queryDir;
	private int intervalSec = 60;
	private HashMap<String, NamedQuery> registeredSQLFiles = new HashMap<String, NamedQuery>();
	private ScriptPool pool;
	
	public ScriptLoader(YgorConfig config) {
		super(config);
	}

	public void doBoot() throws YgorException {
		YgorConfig config = getYgorConfig();

		try {
			this.queryDir = config.f(YgorConfig.SQL_DIR);
			if(!queryDir.isDirectory())
			  throw new IOException(queryDir.getName() + " is not a valid directory");
			
			this.intervalSec = config.i(YgorConfig.SQL_INT);
			this.pool = new ScriptPool();
		} catch (Exception e) {
			throw new YgorException("Failed to initialize script pool", e);
		}

		setRunning(true);
		pool.start();
	}

	public void doHalt() throws YgorException {
		setRunning(false);
		if(pool != null)
		  pool.interrupt();
		pool = null;
	}

  private synchronized boolean isUpdated(NamedQuery sqlf) {
    String name = sqlf.f.getName();
    if (!registeredSQLFiles.containsKey(name) || sqlf.modtime > registeredSQLFiles.get(name).modtime)
      return true;
    
    return false;
  }

	public synchronized NamedQuery getNamedQuery(String name) {
		NamedQuery sqlf = registeredSQLFiles.get(name);

		if (sqlf == null)
			throw new IllegalArgumentException("static query not found: "
					+ name);

		debug("Fetched named query + " + name);
		return sqlf;
	}

	private class ScriptPool extends Thread {
	  public ScriptPool() {
	    super("script pool");
	  }
	  
		public void run() {
			try {
				do {
					updatePool();
					sleep(intervalSec * 1000);
				} while (isRunning());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

    public void updatePool() throws IOException {
      synchronized (ScriptLoader.this) {
        // add updated files
        NamedQuery[] sqlListing = NamedQuery.listSqlFiles(queryDir);

        if (sqlListing == null)
          return;

        for (int i = 0; i < sqlListing.length; i++) {
          if (isUpdated(sqlListing[i])) {
            info("Load script: " + sqlListing[i].name);
            registeredSQLFiles.put(sqlListing[i].name, sqlListing[i]);
          }
        }

        // remove deleted files
        NamedQuery[] registeredFiles = registeredSQLFiles.values().toArray(new NamedQuery[0]);

        for (int i = 0; i < registeredFiles.length; i++) {
          if (!registeredFiles[i].f.exists()) {
            info("Unload script: " + registeredFiles[i].f.getName());
            registeredSQLFiles.remove(registeredFiles[i].f.getName());
          }
        }
      }
    }
	}
}