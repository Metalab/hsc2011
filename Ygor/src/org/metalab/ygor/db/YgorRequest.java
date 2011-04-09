package org.metalab.ygor.db;

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.http.YgorWeb;

public class YgorRequest {
	private String name;
	private HttpServletRequest request;
	private HashMap<String, String> params = new HashMap<String, String>();
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
		web.info("parsing request header");
		Enumeration<String> enumNames = request.getParameterNames();
		String name;
		while (enumNames.hasMoreElements()) {
				String key = enumNames.nextElement();
				String value = request.getParameter(key);
				
				web.debug(
								"request parameter: " + key + " = " + value);
				params.put(key, value);
		}
		String ts = String.valueOf(System.currentTimeMillis() - (24 * 3600000));
		params.put("timemillis", ts);
		web.debug("timemillis: " + ts);
	}

	public String name() {
		return name;
	}

	public YgorQuery execute(String caller) throws YgorException {
	  String name = value("name", true);
	  String query = value("query", true); 

	  try {
	    if (name != null) {
        return db.createPreparedQuery(name,params);
      } else if (query != null) { 
			  return db.createQuery(query); 
			} 
			else
				throw new YgorException(
						"Presence of either ygor-name or ygor-query is manadatory");
		} catch (Exception e) {
			throw new YgorException("Transaction failed", e);
		} 
	}

	public String value(String key) {
		return value(key, false);
	}

	public String value(String key, boolean tolerant) {
		String param = params.get(key);
		if (!tolerant && param == null)
			throw new IllegalArgumentException("HTTP header not found: "
					+ param);
		return param;
	}

	public boolean test(String headerKey, String pattern) {
		return value(headerKey, true).trim().equalsIgnoreCase(pattern);
	}
}
