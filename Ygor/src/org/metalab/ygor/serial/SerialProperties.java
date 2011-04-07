package org.metalab.ygor.serial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.print.event.PrintServiceAttributeEvent;

public class SerialProperties {
	private AtomicReference<Properties> properties = new AtomicReference<Properties>();

	private int databits;
	private int stopbits;
	private int parity;
	private int baud;
	private String device;

	public SerialProperties(File conf) {
		load(conf);
	}

	private void load(File conf) {
		InputStream is = null;
		try {
			is = new FileInputStream(conf);
			Properties newProps = new Properties();
			newProps.load(is);
			properties.compareAndSet(null, newProps);

			// validate properties file
			try {
				device = properties.get().getProperty("device");
				baud = Integer.parseInt(properties.get().getProperty("baud"));
				databits = Integer.parseInt(properties.get().getProperty(
						"databits"));
				stopbits = Integer.parseInt(properties.get().getProperty(
						"stopbits"));
				parity = Integer.parseInt(properties.get()
						.getProperty("parity"));
				if (!(parity == 0 || parity == 1))
					throw new Exception(
							"parity value is out of range (can be only 0 or 1).");
			} catch (Exception e) {
				throw e;
			}
			System.out.println("serial properties loaded.");
		} catch (Exception e) {
			throw new RuntimeException(
					"error loading properties file for tranceiver: " + e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// logger.error(e);
				}
			}
		}
	}

	public void dump(Writer w) throws IOException {
	  PrintWriter pw = new PrintWriter(w);
	  pw.println("device=" + this.device);   
	  pw.println("baud=" + this.baud);
    pw.println("databits=" + this.databits);
    pw.println("stopbits=" + this.stopbits);
    pw.println("parity=" + this.parity);
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    try {
      dump(sw);
    } catch (IOException e) {
    }
    return sw.toString();
  }
	
	public int getDatabits() {
		return databits;
	}

	public int getStopbits() {
		return stopbits;
	}

	public int hasParity() {
		return parity;
	}

	public int getBaud() {
		return baud;
	}

	public String getDevice() {
		return device;
	}
}
