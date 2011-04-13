package org.metalab.ygor.test;

import java.util.Iterator;

import javax.swing.table.AbstractTableModel;
import org.json.JSONArray;
import org.json.JSONObject;

public class LoginModel extends AbstractTableModel {
  private JSONArray data = null;
  private int rowCnt = 0;
  private String[] columnNames = new String[] { "rowid", "src", "dest", "ibutton", "accepted"};
  
  protected LoginModel() {
    new PeriodicUpdate(1000).start();
  }

  public synchronized String getColumnName(int columnIndex) {
    return columnNames[columnIndex];
  }

  public Class<?> getColumnClass(int columnIndex) {
    return String.class;
  }
  
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  public synchronized int getRowCount() {
    return rowCnt;
  }

  public synchronized int getColumnCount() {
    return columnNames.length;
  }

  public synchronized Object getValueAt(int rowIndex, int columnIndex) {
    try {
      return data.getJSONObject(rowIndex).getString(columnNames[columnIndex]);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private synchronized void update(){
    try {
      JSONArray new_data = BuzzerClient.getBuzzerActions().ygorFetch("ls_login.sql");
      int new_rowCnt = 0;
      
      if (new_data != null)
        new_rowCnt = new_data.length();
      
      if(new_rowCnt == rowCnt) {
        JSONObject oldObj;
        JSONObject newObj;
        for (int i = 0; i < new_rowCnt; i++) {
          oldObj = data.getJSONObject(i);
          newObj = new_data.getJSONObject(i);
          Iterator<String> keys = oldObj.keys();
          
          while(keys.hasNext()) {
            String k = keys.next();
            if(!newObj.getString(k).equals(oldObj.getString(k))) {
              data = new_data;
              rowCnt = new_rowCnt;
              fireTableDataChanged();
              break;
            }
          }
        }
      } else {
        data = new_data;
        rowCnt = new_rowCnt;
        fireTableDataChanged();
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
      while (true) {
        try {
          LoginModel.this.update();
        } catch (Exception e) {
          e.printStackTrace();
        }
        try {
          Thread.sleep(interval);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
