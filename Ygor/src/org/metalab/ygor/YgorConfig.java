package org.metalab.ygor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.metalab.ygor.util.Parameter;
import org.metalab.ygor.util.Parameter.TYPES;

public class YgorConfig extends Properties {
  private static final long serialVersionUID = -7008838742588871032L;
  
  public final static Parameter DB_DRIVER = new Parameter("db.driver", TYPES.STRING);
  public final static Parameter DB_URL = new Parameter("db.url", TYPES.STRING);
  public final static Parameter DB_SCHEMA = new Parameter("db.schema", TYPES.FILE);
  public final static Parameter DB_ALLOW_CREATE = new Parameter("db.allowCreate", TYPES.BOOLEAN);
  public final static Parameter SQL_DIR = new Parameter("sql.dir", TYPES.FILE);
  public final static Parameter SQL_INT = new Parameter("sql.interval", TYPES.INTEGER);
  public final static Parameter HTTP_PORT = new Parameter("http.port", TYPES.INTEGER);
  public final static Parameter HTTP_HOST = new Parameter("http.host", TYPES.STRING);
  public final static Parameter HTTP_SERVLET_MAP = new Parameter("http.servlets", TYPES.FILE);
  public final static Parameter HTTP_ALIASES = new Parameter("http.aliases", TYPES.FILE);
  public final static Parameter DEBUG = new Parameter("debug", TYPES.BOOLEAN);
  public final static Parameter SERIAL_CONF = new Parameter("serial.conf", TYPES.FILE);
  public final static Parameter SERIAL_RESEND_INTERVAL = new Parameter("serial.resendInterval", TYPES.LONG);
  
	public YgorConfig() {
		set(HTTP_HOST, "localhost");
		set(HTTP_PORT, 8348);
		set(HTTP_ALIASES, "aliases.conf");
		set(HTTP_SERVLET_MAP, "servlets.conf");
		set(SQL_DIR, "sql");
		set(SQL_INT, 60);
		set(DB_SCHEMA, "schema");
		set(DB_DRIVER, "org.sqlite.JDBC");
		set(DB_URL, "jdbc:sqlite:ygor.db");
	  set(DEBUG, false);
	  set(SERIAL_CONF, "serial.properties");
	  set(SERIAL_RESEND_INTERVAL, "50");
	}

	public YgorConfig(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		load(in);
		in.close();
	}

	public Float fl(Parameter p) {
		return fl(p, false);
	}

	public Double d(Parameter p) {
		return d(p, false);
	}

	public Boolean b(Parameter p) {
		return b(p, false);
	}

	public String s(Parameter p) {
		return s(p, false);
	}

	public File f(Parameter p) {
		return f(p, false);
	}

	public Integer i(Parameter p) {
		return i(p, false);
	}

	public Long l(Parameter p) {
		return l(p, false);
	}

	public Float fl(Parameter p, boolean envParam) {
		return (Float) o(p, envParam);
	}

	public Double d(Parameter p, boolean envParam) {
		return (Double) o(p, envParam);
	}

	public Boolean b(Parameter p, boolean envParam) {
		return (Boolean) o(p, envParam);
	}

	public String s(Parameter p, boolean envParam) {
		return (String) o(p, envParam);
	}

	public File f(Parameter p, boolean envParam) {
		return (File) o(p, envParam);
	}

	public Integer i(Parameter p, boolean envParam) {
		return (Integer) o(p, envParam);
	}

	public Long l(Parameter p, boolean envParam) {
		return (Long) o(p, envParam);
	}

	private Object o(Parameter p, boolean envParam) {
		String param;
		if (!has(p, envParam)) {
			if (envParam)
				param = System.getProperty(p.name());
			else
				param = getProperty(p.name());
			
			return p.typeIt(param);
		}

		return null;
	}

	public void dump(PrintStream out) {
		try {
		  // make the dump more pretty by preceding a new line
		  out.print(System.getProperty("line.separator"));

		  this.store(out,"Igor configuration");
    } catch (IOException e) {
      e.printStackTrace();
    }
	}
	
	public boolean has(Parameter p, boolean envParam) {
		if(p == null)
			throw new NullPointerException();
		
		
		Properties properties;

		if (envParam)
			properties = System.getProperties();
		else
			properties = this;

		if(!properties.containsKey(p))
			return false;
		else
			return true;
	}

	public void set(Parameter p, Object value) {
		Object typed = p.typeIt(value);
		String strVal = null;

		if (typed != null)
			strVal = typed.toString();

		setProperty(p.name(), strVal);
	}
}
