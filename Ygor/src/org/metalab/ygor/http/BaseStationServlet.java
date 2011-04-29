package org.metalab.ygor.http;

import java.io.OutputStream;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.serial.packet.Dispatcher;

public class BaseStationServlet extends YgorServlet {
  private Dispatcher dispatcher;
  
  public BaseStationServlet() {
    super("text/html");
    this.dispatcher = YgorDaemon.baseStation().getDispatcher();
  }

  protected void process(YgorRequest query, OutputStream out) {
    String cmd = query.value("cmd");
    try {
        YgorDaemon.baseStation().transmit(cmd);
    } catch (Exception e) {
      throw new YgorException("Unable to execute controller command: " + cmd, e);
    }
  }
}
