package org.metalab.ygor.test;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.swing.AbstractAction;

import org.json.JSONArray;
import org.json.JSONTokener;

public class BuzzerActions {
  private LoginTable lt;
  private String ygorUrl;
  private String baseCmdUrl;
 
  public BuzzerActions(String host, int port, LoginTable bt) {
    this.lt = bt;
    this.ygorUrl = "http://" + host + ":" + port + "/ygor?name=";
    this.baseCmdUrl = "http://" + host + ":" + port + "/base?cmd=";
  }
  
  public void initBaseStation() {
    this.baseCmd("M05");
  }
  
  public class AcceptLogin extends AbstractAction {
    public AcceptLogin() {
      super("Accept");
    }
    
    public void actionPerformed(ActionEvent e) {
      ygorFetchBySrc("accept_login.sql", lt.getSelectedSrc());
    }
  }

  public JSONArray ygorFetch(String name) {
    return this.ygorFetch(name, (HashMap<String, String>) null);
  }
  
  public JSONArray ygorFetchBySrc(String name, String src) {
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("src", src);
    return this.ygorFetch(name, params);
  }
  
  public JSONArray ygorFetchByRowID(String name, String rowid) {
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("rowid", rowid);
    return this.ygorFetch(name, params);
  }

  public JSONArray ygorFetch(String name, HashMap<String, String> params) {
    InputStream in = null;
    try {
      StringBuilder sbURL = new StringBuilder(ygorUrl + name);
      if (params != null) {
        Object[] keys = (Object[]) params.keySet().toArray();
        for (int i = 0; i < keys.length; i++)
          sbURL.append('&').append(keys[i]).append('=').append(params.get(keys[i]));
      }
      String urlString = sbURL.toString();
      System.out.println("action: " + urlString);
      in = new URL(urlString).openStream();
      return new JSONArray(new JSONTokener(in));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if(in != null)
          in.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
  
  public void baseCmd(String cmd) {
    try {
      StringBuilder sbURL = new StringBuilder(baseCmdUrl + cmd);
      String urlString = sbURL.toString();
      System.out.println("action: " + urlString);
      InputStream in = new URL(urlString).openStream();
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

