package org.metalab.ygor.client;

import org.metalab.ygor.Service;
import org.metalab.ygor.YgorConfig;
import org.metalab.ygor.YgorException;

public class Client extends Service {
  private Outgoing outgoing;
  public Client(YgorConfig config) {
    super(config);
  }
  
  public Outgoing getOutgoing() {
    return outgoing;
  }

  public void doBoot() throws YgorException {
    try {
      this.outgoing = new Outgoing(getYgorConfig());
      this.outgoing.boot();
    } catch (Exception e) {
      throw new YgorException("Failed to boot outgoing", e);
    }
  }

  public void doHalt() throws YgorException {
    try {
      if (outgoing != null)
        outgoing.halt();
    } catch (Exception e) {
      warn("Unable to halt outgoing", e);
    }
    outgoing = null;
  }
}
