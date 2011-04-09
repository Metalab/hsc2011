package org.metalab.ygor.serial;

import gnu.io.*;
import java.io.*;
import java.util.TooManyListenersException;

public class SerialPortInputStream extends InputStream implements SerialPortEventListener{
	private PipedInputStream sink;
	private PipedOutputStream src;

	private InputStream underlying;

    public SerialPortInputStream(SerialPort p) throws IOException, TooManyListenersException {
        this.underlying = p.getInputStream();
        p.addEventListener(this);
		p.notifyOnDataAvailable(true);
        this.sink = new PipedInputStream();
        this.src = new PipedOutputStream(sink);
    }

    public void close() throws IOException {
    	underlying.close();
    	sink.close();
    }
    
    public void serialEvent(SerialPortEvent arg0) {
        int data;

        try {
            while ((data = underlying.read()) > -1) {
            	src.write(data);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

	public int read() throws IOException {
		return sink.read();
	}
}
