package org.metalab.ygor.serial;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

public class NullInputStream extends InputStream {
  public int read() throws IOException {
    return 1;
  }
  
  public int available() throws IOException {
    return 1;
  }
  
  public void close() throws IOException {
  }
}
