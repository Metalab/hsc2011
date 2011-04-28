package org.metalab.ygor.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.serial.packet.Dispatcher;
import org.metalab.ygor.serial.packet.Packet;

public class BaseStation extends Service {
  private SerialPipeProcess serialPipeProc;
  private InputStream in;
  private OutputStream out;

  private Object rxmutex = new Object();
  private Object txmutex = new Object();

  private Dispatcher dispatcher;
  
  public BaseStation(YgorConfig config) {
    super(config);
  }
  
  public Dispatcher getDispatcher() {
    return dispatcher;
  }

  private void init() throws IOException {
    serialPipeProc = new SerialPipeProcess(getYgorConfig());
    this.in = serialPipeProc.getInputStream();
    this.out = serialPipeProc.getOutputStream();
    
    serialPipeProc.addExitHook(new Runnable() {
      public void run() {
        if(BaseStation.this.isRunning())
          BaseStation.this.halt();
      }
    });
  }

  public void transmit(String s) throws IOException {
    synchronized (txmutex) {
      debug("TX: " + s);
      
      out.write(s.getBytes());
      out.write('\n');
    }
  }
  
  public void transmit(Packet pkt) throws IOException {
    synchronized (txmutex) {
      transmit(pkt.toString());
    }
  }

  public Packet receive() throws IOException {
    synchronized (rxmutex) {
      StringBuilder sb = new StringBuilder();

      int d;

      while (((d = in.read()) != -1 && d != '\n'))
        sb.append((char) d);

      String s = sb.toString();

      debug("RX: " + s);
      if (!s.trim().startsWith("*") && !s.startsWith("-")
          && !s.startsWith("user") && !(s.trim().length() == 0)) {
        return Packet.parsePacket(s);
      }

      return null;
    }
  }

  public void doBoot() throws YgorException {
    try {
      init();
    } catch (Exception e) {
      throw new YgorException("Failed to open serial device", e);
    }

    try {
      this.dispatcher = new Dispatcher(getYgorConfig());
      this.dispatcher.boot();
    } catch (Exception e) {
      throw new YgorException("Failed to boot dispatcher", e);
    }
  }

  public void doHalt() throws YgorException {
    try {
      if (in != null)
        in.close();
    } catch (Exception e) {
      warn("Unable to input stream", e);
    }

    try {
      if (out != null)
        out.close();
    } catch (Exception e) {
      warn("Unable to close output stream", e);
    }

    try {
      if (serialPipeProc != null)
        serialPipeProc.destroy();
    } catch (Exception e) {
      warn("Unable to close port", e);
    }

    try {
      if (dispatcher != null)
        dispatcher.halt();
    } catch (Exception e) {
      warn("Unable to halt dispatcher", e);
    }
    in = null;
    out = null;
    serialPipeProc = null;
    dispatcher = null;
  }
}
