package org.metalab.ygor;

import java.io.File;
import org.metalab.ygor.db.YgorDB;
import org.metalab.ygor.http.YgorWeb;
import org.metalab.ygor.serial.BaseStation;

public class YgorDaemon extends Service {
	private static YgorDaemon instance;
	private static YgorDB db;
	private static YgorWeb web; 
	private static BaseStation serial;
	
	public YgorDaemon(YgorConfig config){
		super(config);
		logConfig();
		YgorDaemon.instance = this;
	}

	public synchronized void doBoot() throws YgorException {
		YgorConfig config = YgorDaemon.instance.getYgorConfig();
		
		Runtime.getRuntime().addShutdownHook(new Thread("shutdown hook"){
			public void run() {
				YgorDaemon.shutdown();
			}
		});
		
    YgorDaemon.serial = new BaseStation(config);
    
		YgorDaemon.db = new YgorDB(config);
		YgorDaemon.db.boot();

		YgorDaemon.web = new YgorWeb(config);
		YgorDaemon.web.boot();
		
    YgorDaemon.serial.boot();
	}
	
	public synchronized void doHalt() {
		if(db != null)
			db.halt();
		if(web != null)
			web.halt();
    if(serial != null)
      serial.halt();

		web = null;
		db = null;
		serial = null;
		
		System.exit(0);
	}

	public static void shutdown() {
		instance.info("shutdown");
		
		if(instance != null)
			instance.halt();
	}

	public static YgorDB db() {
		if(db == null)
			throw new YgorException("DB not initialized");

		return db;
	}

	public static YgorWeb web() {
		if(web == null)
			throw new YgorException("Webserver not initialized");

		return web;
	}
	
  public static BaseStation baseStation() {
    if (serial == null)
      throw new YgorException("Serial device not initialized");

    return serial;
  }
  
	public static void printUsageAndExit() {
		System.out
				.println("Usage: ygor <config file> --create");
		System.exit(1);
	}
	

	public static void main(String[] args) {
		try {
			YgorConfig config = null;

			if (args.length < 1 || args.length > 2)
				printUsageAndExit();

			if (args[0].equals("--create")) {
				config = new YgorConfig(new File(args[1]));
				config.set(YgorConfig.DB_ALLOW_CREATE, "true");
			} else {
				config = new YgorConfig(new File(args[0]));
				if (args.length > 1 && args[1].equals("--create"))
					config.set(YgorConfig.DB_ALLOW_CREATE, "true");
				else
					config.set(YgorConfig.DB_ALLOW_CREATE, "false");
			}

			new YgorDaemon(config).boot();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}