package org.metalab.ygor.serial;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

public class NullInputStream extends InputStream {
  Semaphore blockUntilClose = new Semaphore(0);
  public int read() throws IOException {
    try {
      blockUntilClose.acquire();
    } catch (InterruptedException e) { e.printStackTrace(); }
    return -1;
  }
  
  public void close() throws IOException {
    blockUntilClose.release();
  }

}
