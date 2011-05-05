package org.metalab.ygor.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

public class NullOutputStream extends OutputStream {
  Semaphore blockUntilClose = new Semaphore(0);
  public void close() throws IOException {
    blockUntilClose.release();
  }

  @Override
  public void write(int b) throws IOException {
    try {
      blockUntilClose.acquire();
    } catch (InterruptedException e) { e.printStackTrace(); }
    throw new IOException("Nullstream closed");
  }
}
