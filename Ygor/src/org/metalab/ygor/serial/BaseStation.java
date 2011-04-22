package org.metalab.ygor.serial;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.serial.packet.Dispatcher;
import org.metalab.ygor.serial.packet.Packet;

public class BaseStation extends Service {
  private Process serialPipeProc;
  private BufferedInputStream in;
  private BufferedOutputStream out;

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
    File serialConf = getYgorConfig().f(YgorConfig.SERIAL_CONF);
    SerialProperties props = new SerialProperties(serialConf);
    info(props.toString());

    String[] cmd = new String[] { "./serial-filter",
        "--port=" + props.getPort(),
        "--baud=" + props.getBaud(),
        "--parity=" + props.getParity(), 
        "--databits=" + props.getDatabits(), 
        "--stopbits=" + props.getStopbits() };

    this.serialPipeProc = Runtime.getRuntime().exec(cmd);
    this.in = (BufferedInputStream)serialPipeProc.getInputStream();
    this.out = (BufferedOutputStream)serialPipeProc.getOutputStream();
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

  public Packet receive() {
    synchronized (rxmutex) {
      StringBuilder sb = new StringBuilder();
      try {
        int d;

        while (((d = in.read()) != -1 && d != '\n') || sb.length() == 0)
          sb.append((char) d);

        String s = sb.toString();
        
        debug("RX: " + s);
        if(!s.trim().startsWith("*") && !s.startsWith("-") && !s.startsWith("user")) {
          return Packet.parsePacket(s);
        }
      } catch (Exception e) {
        warn("Unparseable message detected: " + sb, e);
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

    in = null;
    out = null;
    serialPipeProc = null;
  }
}
