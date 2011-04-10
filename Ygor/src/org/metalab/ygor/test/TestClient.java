package org.metalab.ygor.test;

import javax.swing.JFrame;
import javax.swing.JTable;

public class TestClient extends JFrame {
  public TestClient(String host, int port) {
    getContentPane().add(new JTable(new BuzzerModel(host,port)));
    setSize(400,300);
    setVisible(true);
  }
  
  public static void main(String[] args) {
    new TestClient(args[0], Integer.parseInt(args[1]));
  }
}
