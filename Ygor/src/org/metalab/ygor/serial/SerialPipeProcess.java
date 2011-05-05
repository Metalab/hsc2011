package org.metalab.ygor.serial;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import org.metalab.ygor.YgorConfig;

public class SerialPipeProcess extends Process {
	private Process serialPipeProc;
	private InputStream in;
	private OutputStream out;
	
	public SerialPipeProcess(YgorConfig config) throws IOException {
    SerialProperties props = new SerialProperties(config.f(YgorConfig.SERIAL_CONF));
    String[] cmd = props.getExternalCommand();
    this.serialPipeProc = Runtime.getRuntime().exec(cmd);
    try {
      this.in = getDirectInputStream(this.serialPipeProc);
      this.out = getDirectOutputStream(this.serialPipeProc);
    } catch (Exception e) {
      throw new IOException("Unable to obtain direct stream", e);
    }
	}

	public void addExitHook(Runnable hook) {
	  //new ProcObserver(hook).start();
	}
	
	public void destroy() {
	  serialPipeProc.destroy();
	  try { in.close(); } catch (IOException e) {}
	  try { out.close(); } catch (IOException e) {}
	}

	public int exitValue() {
		return serialPipeProc.exitValue();
	}

	public InputStream getErrorStream() {
		return serialPipeProc.getErrorStream();
	}

	public InputStream getInputStream() {
		return this.in;
	}

	public OutputStream getOutputStream() {
		return this.out;
	}

	public int waitFor() throws InterruptedException {
		return serialPipeProc.waitFor();
	}

  private static FileInputStream getDirectInputStream(Process p)
      throws IOException, NoSuchFieldException, IllegalAccessException {
    BufferedInputStream bIn = (BufferedInputStream) p.getInputStream();

    Field fDirectIn = bIn.getClass().getSuperclass().getDeclaredField("in");
    fDirectIn.setAccessible(true);
    FileInputStream fIn = (FileInputStream) fDirectIn.get(bIn);
    fDirectIn.set(bIn, new NullInputStream());
    return fIn;
  }

  private static FileOutputStream getDirectOutputStream(Process p)
      throws IOException, NoSuchFieldException, IllegalAccessException {
    BufferedOutputStream bOut = (BufferedOutputStream) p.getOutputStream();

    Field fDirectOut = bOut.getClass().getSuperclass().getDeclaredField("out");
    fDirectOut.setAccessible(true);
    FileOutputStream fOut = (FileOutputStream) fDirectOut.get(bOut);
    fDirectOut.set(bOut, new NullOutputStream());
    return fOut;
  }
  
  private class ProcObserver extends Thread {
    private Runnable hook;

    public ProcObserver(Runnable hook) {
      this.hook = hook;
    }

    public void run() {
      try {
        SerialPipeProcess.this.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      new Thread(hook).start();
    }
  }
}
