package org.metalab.ygor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

public abstract class Service {
	private boolean running = false;
	private Logger logger;
	private YgorConfig config;
	
	public Service(YgorConfig config) {
		this.config = config;
		this.logger = Logger.getLogger(getClass().getName()); 
	}

	public synchronized void boot() throws YgorException {
		info("boot");
		
		if(isRunning())
			throw new YgorException("Already running");
		try {
			doBoot();
			setRunning(true);
		} catch (Exception e) {
			error("Failed to boot", e);
			doHalt();
			throw new YgorException("Halting due to previous errors: ", e);
		}
	}

	public synchronized void halt() {
		info("halt");
		
		if(!isRunning())
			warn("Not running");

		try {
			doHalt();
		} catch (YgorException e) {
			warn("Failed to halt",e);
		}
		setRunning(false);
	}

	public synchronized void setRunning(boolean running) {
		this.running = running;
	}
	
	public synchronized boolean isRunning() {
		return running;
	}

	public abstract void doBoot() throws YgorException;
	public abstract void doHalt() throws YgorException;

	public YgorConfig getYgorConfig() {
		return config;
	}

  public void debug(String message) {
    if (config.b(YgorConfig.DEBUG))
      logger.debug(message);
  }
	
	public void error(String message){
		logger.error(message);
	}

	public void warn(String message){
		logger.warn(message);
	}

	public void debug(String message, Throwable cause){
		if(config.b(YgorConfig.DEBUG))
		  logger.debug(message, cause);
	}
	
	public void error(String message, Throwable cause){
		logger.error(message, cause);
	}

	public void warn(String message, Throwable cause){
		logger.warn(message, cause);
	}

	public void info(String message){
		logger.info(message);
	}

	public void logConfig(){
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		getYgorConfig().dump(new PrintStream(buffer));
		info(new String(buffer.toByteArray()));
	}
}