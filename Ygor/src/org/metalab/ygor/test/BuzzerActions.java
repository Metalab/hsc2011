package org.metalab.ygor.test;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.HashMap;

import javax.swing.AbstractAction;

import org.json.JSONArray;
import org.json.JSONTokener;

public class BuzzerActions {
  private LoginTable lt;
  private String baseUrl;
 
  public BuzzerActions(String host, int port, LoginTable bt) {
    this.lt = bt;
    this.baseUrl = "http://" + host + ":" + port + "/ygor?name=";
  }
  
  public class AcceptLogin extends AbstractAction {
    public AcceptLogin() {
      super("Accept");
    }
    
    public void actionPerformed(ActionEvent e) {
      ygorFetch("accept_login.sql", lt.getSelectedRowID());
    }
  }

  public JSONArray ygorFetch(String name) {
    return this.ygorFetch(name, (HashMap<String, String>) null);
  }
  
  public JSONArray ygorFetch(String name, String rowid) {
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("rowid", rowid);
    return this.ygorFetch(name, params);
  }
  
  public JSONArray ygorFetch(String name, HashMap<String, String> params) {
    try {
      StringBuilder sbURL = new StringBuilder(baseUrl + name);
      if (params != null) {
        Object[] keys = (Object[]) params.keySet().toArray();
        for (int i = 0; i < keys.length; i++)
          sbURL.append('&').append(keys[i]).append('=').append(params.get(keys[i]));
      }
      String urlString = sbURL.toString();
      System.out.println("action: " + urlString);
      return new JSONArray(new JSONTokener(new URL(urlString).openStream()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}

