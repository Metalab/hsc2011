package org.metalab.ygor.test;

import javax.swing.JTable;

public class LoginTable extends JTable {
  public LoginTable() {
    super(new LoginModel());
    setVisible(true);
  }

  public String getSelectedSrc() {
    return getModel().getValueAt(getSelectedRow(), 0).toString();
  }

  public String[] getSelectedData() {
    String[] data = new String[getColumnCount()];
    int selRow = getSelectedRow();
    
    for (int i = 0; i < data.length; i++) {
      data [i] = getModel().getValueAt(selRow, i).toString();  
    }
    
    return data;
  }
}