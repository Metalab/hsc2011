package org.metalab.ygor.util;

import java.io.*;
import java.util.HashMap;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;

public class ScriptLoader extends Service {
	private File queryDir;
	private int intervalSec = 60;
	private HashMap<String, SQLFile> registeredSQLFiles = new HashMap<String, SQLFile>();
	private ScriptPool pool;
	
	public ScriptLoader(YgorConfig config) {
		super(config);
	}

	public synchronized void doBoot() throws YgorException {
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

	public synchronized void doHalt() throws YgorException {
		setRunning(false);
		if(pool != null)
		  pool.interrupt();
		pool = null;
	}

  private synchronized boolean isUpdated(SQLFile sqlf) {
    String name = sqlf.f.getName();
    if (!registeredSQLFiles.containsKey(name) || sqlf.modtime > registeredSQLFiles.get(name).modtime)
      return true;
    
    return false;
  }

	public synchronized String getNamedQuery(String name) {
		SQLFile sqlf = registeredSQLFiles.get(name);

		if (sqlf == null)
			throw new IllegalArgumentException("static query not found: "
					+ name);

		debug("Fetched named query + " + name);
		return sqlf.query;
	}

	private class ScriptPool extends Thread {
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
			// add updated files
			SQLFile[] sqlListing = SQLFile.listSqlFiles(queryDir);

			if (sqlListing == null)
				return;

      for (int i = 0; i < sqlListing.length; i++) {
        if (isUpdated(sqlListing[i])) {
          info("Load script: " + sqlListing[i].name);
          registeredSQLFiles.put(sqlListing[i].name, sqlListing[i]);
        }
      }

			// remove deleted files
			SQLFile[] registeredFiles = registeredSQLFiles.values().toArray(
					new SQLFile[0]);

			for (int i = 0; i < registeredFiles.length; i++) {
				if (!registeredFiles[i].f.exists()) {
					info("Unload script: " + registeredFiles[i].f.getName());
					registeredSQLFiles.remove(registeredFiles[i].f.getName());
				}
			}
		}
	}
}