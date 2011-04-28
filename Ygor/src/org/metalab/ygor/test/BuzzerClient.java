package org.metalab.ygor.test;

import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class BuzzerClient extends JFrame {
  private LoginTable loginTable;
  private static BuzzerActions actions;
  
  public BuzzerClient(String host, int port) {
    super("Edubuzzer test client");
    
    setSize(150, 150);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        dispose();
        System.exit(0);
      }
    });
    init(host, port);
    pack();
    setVisible(true);
  }
  
  public static BuzzerActions getBuzzerActions() {
    return actions;
  }

  private void init(String host, int port) {
    getContentPane().setLayout(new FlowLayout());
    loginTable = new LoginTable();
    actions = new BuzzerActions(host,port,loginTable);
    actions.initBaseStation();
    JScrollPane pane = new JScrollPane(loginTable);
    getContentPane().add(pane);
    getContentPane().add(new JButton(getBuzzerActions().new AcceptLogin()));
  }

  public static void main(String[] args) {
    new BuzzerClient(args[0], Integer.parseInt(args[1]));
  }
}
