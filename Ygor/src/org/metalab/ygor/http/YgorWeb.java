package org.metalab.ygor.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;


import Acme.Serve.Serve;


public class YgorWeb extends Service {
	private Server srv;
	
	public YgorWeb(YgorConfig config) {
		super(config);
	}

	public synchronized void doBoot() throws YgorException {
		try {
			this.srv = new Server();
			YgorConfig config = getYgorConfig();
			java.util.Properties properties = new java.util.Properties();
			properties.put("port", config.i(YgorConfig.HTTP_PORT));
			properties.put(Acme.Serve.Serve.ARG_BINDADDRESS, config.s(YgorConfig.HTTP_HOST));
			properties.put(Acme.Serve.Serve.ARG_NOHUP, "nohup");

			srv.arguments = properties;
			srv.addDefaultServlets(null);

			loadAliases();
			loadServlets();

			info("serve");
			new Thread("HTTP Server") {
				public void run()	{					
					if (srv.serve() != 0)
						System.exit(0);
				}
			}.start();

		} catch (Exception e) {
			throw new YgorException("Failed to initialize ServletContainer", e);
		}
	}

	public synchronized void doHalt() throws YgorException {
		try {
			srv.notifyStop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		srv.destroyAllServlets();
	}

	public void loadAliases() throws IOException {
		info("load aliases");
		Serve.PathTreeDictionary dict = new Serve.PathTreeDictionary();
		File aliases = getYgorConfig().f(YgorConfig.HTTP_ALIASES);
		BufferedReader reader = new BufferedReader(new FileReader(aliases));
		String line;
		String[] alias;
		while ((line = reader.readLine()) != null) {
			debug("http alias config input line: " + line);
			alias = line.split("\\s");
			if (alias.length != 2)
				throw new YgorException("Malformed alias detected: " + line);
			
			info("http alias: " + alias[0] + " " + alias[1]);
			File localDir = new File(alias[1]);
			
			if(!localDir.exists())
				throw new FileNotFoundException("Mapped directory doesn't exist");
			else if(!localDir.isDirectory())
				throw new FileNotFoundException("Mapped directory doesn't exist");
				
			dict.put(alias[0], new File(alias[1]));
		}
		srv.setMappingTable(dict);
	}

	public void loadServlets() throws IOException {
		info("load servlets");
		File map = getYgorConfig().f(YgorConfig.HTTP_SERVLET_MAP);
		BufferedReader reader = new BufferedReader(new FileReader(map));
		String line;
		String[] mapping;
		while ((line = reader.readLine()) != null) {
			debug("http servlet mapping input line: " + line);
			mapping = line.split("\\s");
			if (mapping.length != 2)
				throw new YgorException("Malformed mapping detected: " + line);

			YgorServlet servlet = instantiateServlet(mapping[1]);
			info("http servlet mapping: " + mapping[0] + " " + mapping[1]);
			srv.addServlet(mapping[0], servlet);
		}
	}

	private YgorServlet instantiateServlet(String className) {
		try {
			return (YgorServlet) Class.forName(className).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public class Server extends Acme.Serve.Serve {
		public void setMappingTable(PathTreeDictionary mappingtable) {
			super.setMappingTable(mappingtable);
		}

		public void addWarDeployer(String deployerFactory, String throttles) {
			super.addWarDeployer(deployerFactory, throttles);
		}
	}
}
