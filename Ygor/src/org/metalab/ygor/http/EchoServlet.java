package org.metalab.ygor.http;

import java.io.OutputStream;

import org.apache.commons.codec.net.URLCodec;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorRequest;

public class EchoServlet extends YgorServlet {
  public EchoServlet () {
    super("text/html");
  }

  protected void process(YgorRequest query, OutputStream out) {
    String rp = query.value("Response-Body");
    try {
      out.write(URLCodec.decodeUrl(rp.getBytes()));
    } catch (Exception e) {
      throw new YgorException("Unable to echo: " + rp, e);
    }
  }
}



