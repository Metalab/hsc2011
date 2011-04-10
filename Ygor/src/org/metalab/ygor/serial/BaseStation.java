package org.metalab.ygor.serial;

import gnu.io.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.serial.packet.Dispatcher;
import org.metalab.ygor.serial.packet.Packet;

public class BaseStation extends Service {
  private InputStream in;
  private OutputStream out;
  private SerialPort serialPort;

  private Object rxmutex = new Object();
  private Object txmutex = new Object();

  private Dispatcher dispatcher;
  
  public BaseStation(YgorConfig config) {
    super(config);
  }
  
  public Dispatcher getDispatcher() {
    return dispatcher;
  }

  private void init() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, TooManyListenersException, IOException {
    File serialConf = getYgorConfig().f(YgorConfig.SERIAL_CONF);
    SerialProperties props = new SerialProperties(serialConf);
    info(props.toString());

    CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(props.getDevice());
    if (portIdentifier.isCurrentlyOwned()) {
      throw new RuntimeException("Port is currently in use");
    } else {
      CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

      if (commPort instanceof SerialPort) {
        this.serialPort = (SerialPort) commPort;
        this.serialPort.setSerialPortParams(props.getBaud(), props.getDatabits(), props.getStopbits(), props.getParity());

        this.in = new SerialPortInputStream(serialPort);
        this.out = serialPort.getOutputStream();
      } else {
        throw new RuntimeException("Not a RS323 port: " + props.getDevice());
      }
    }
  }

  public void transmit(String s) throws IOException {
    synchronized (txmutex) {
      debug("TX: " + s);
      out.write(s.getBytes());
      out.write('\n');
    }
  }
  
  public void transmit(Packet msg) throws IOException {
    synchronized (txmutex) {
      String msgString = msg.toString();
      debug("TX: " + msgString);
      out.write(msgString.getBytes());
    }
  }

  public Packet receive() {
    synchronized (rxmutex) {
      StringBuilder sb = new StringBuilder();
      try {
        int d;

        while ((d = in.read()) != -1 && d != '\n')
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
      if (serialPort != null)
        serialPort.close();
    } catch (Exception e) {
      warn("Unable to close port", e);
    }

    in = null;
    out = null;
    serialPort = null;
  }
}
