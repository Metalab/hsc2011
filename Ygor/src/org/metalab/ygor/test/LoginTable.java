package org.metalab.ygor.test;

import javax.swing.JTable;

public class LoginTable extends JTable {
  public LoginTable(){
    super(new LoginModel());
    setVisible(true);
  }
  
   public String getSelectedSrc() {
    return getModel().getValueAt(getSelectedRow(), 0).toString();
  }
}