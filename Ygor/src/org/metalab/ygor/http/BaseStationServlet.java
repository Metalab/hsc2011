package org.metalab.ygor.http;

import java.io.OutputStream;

import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.serial.Incoming;

public class BaseStationServlet extends YgorServlet {
  public BaseStationServlet() {
    super("text/html");
  }

  protected void process(YgorRequest query, OutputStream out) {
    String cmd = query.value("cmd");
    trace(cmd);
    try {
        YgorDaemon.baseStation().transmit(cmd);
    } catch (Exception e) {
      throw new YgorException("Unable to execute controller command: " + cmd, e);
    }
  }
}
