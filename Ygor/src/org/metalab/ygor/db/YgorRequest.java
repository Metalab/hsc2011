package org.metalab.ygor.db;

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.http.YgorWeb;
import org.metalab.ygor.util.ParameterMap;

public class YgorRequest implements ParameterMap {
	private String name;
	private HttpServletRequest request;
	private HashMap<String, Object> params = new HashMap<String, Object>();
	private YgorDB db;
	private YgorWeb web;

	public YgorRequest(HttpServletRequest request)
			throws IllegalArgumentException, YgorException {
		this.request = request;
		this.db = YgorDaemon.db();
		this.web = YgorDaemon.web();
		parseHeader();
	}

	private void parseHeader() {
		web.trace("parsing request header");
		Enumeration<String> enumNames = request.getParameterNames();
		
		while (enumNames.hasMoreElements()) {
				String key = enumNames.nextElement();
				String value = request.getParameter(key);
				
				web.trace(
								"request parameter: " + key + " = " + value);
				params.put(key, value);
		}
		String ts = String.valueOf(System.currentTimeMillis() - (24 * 3600000));
		params.put("timemillis", ts);
		web.trace("timemillis: " + ts);
	}

	public String name() {
		return name;
	}

	public YgorQuery execute() throws YgorException {
	  String name = value("name", true); 

	  try {
	    if (name != null)
        return db.createPreparedQuery(name);
			else
				throw new YgorException(
						"parameter name is mandatory");
		} catch (Exception e) {
			throw new YgorException("Transaction failed", e);
		} 
	}

	public String value(String key) {
		return value(key, false);
	}

	public String value(String key, boolean tolerant) {
		Object param = params.get(key);
		if (!tolerant && param == null)
			throw new IllegalArgumentException("HTTP header not found: "
					+ param);
		return param.toString();
	}
	
	public HashMap<String, Object> getParameterMap() {
	  return (HashMap<String, Object>)params.clone();
	}

	public boolean test(String headerKey, String pattern) {
		return value(headerKey, true).trim().equalsIgnoreCase(pattern);
	}
}
