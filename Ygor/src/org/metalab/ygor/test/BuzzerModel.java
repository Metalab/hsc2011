package org.metalab.ygor.test;

import java.net.URL;

import javax.swing.table.AbstractTableModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BuzzerModel extends AbstractTableModel {
  private String host;
  private int port;
  private JSONArray json = null;
  private int rowCnt = 0;
  private int colCnt = 0;
  private String[] columnNames;
  
  public BuzzerModel(String host, int port) {
    this.host = host;
    this.port = port;
    new PeriodicUpdate(1000).start();
  }

  public int getRowCount() {
    return rowCnt;
  }

  public int getColumnCount() {
    return colCnt;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    try {
      return json.getJSONObject(rowIndex).getString(columnNames[columnIndex]);
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private synchronized void update()
  {
    try {
      String lsLogin = "http://" + host + ":" + port + "/ygor?name=" + "ls_login.sql";
      System.out.println("update: " + lsLogin);
      URL url = new URL(lsLogin);
      JSONArray jsonArr = new JSONArray(new JSONTokener(url.openStream()));
      int len = jsonArr.length();
      if(len > 0) {
        rowCnt = len;
        columnNames = JSONObject.getNames(jsonArr.getJSONObject(0));
        colCnt = columnNames.length;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private class PeriodicUpdate extends Thread {
    private long interval;
    public PeriodicUpdate(long interval) {
       this.interval = interval;
    }
    public void run() {
      try {
        sleep(interval);
        BuzzerModel.this.update();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
