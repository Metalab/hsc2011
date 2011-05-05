package org.metalab.ygor.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

public class NullOutputStream extends OutputStream {
  public void close() throws IOException {
  }

  public void write(int b) throws IOException {
  }
}
