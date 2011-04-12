package org.metalab.ygor.serial.packet;

import java.io.PrintWriter;
import java.io.StringWriter;

public class VMStatus extends Payload {
  boolean start = false;
  boolean stop = false;
  
  boolean set_ip = false;
  short[] ip = null;
  
  boolean set_rgb = false;
  short[] rgb = null;
  
  boolean set_buzzer = false;
  int buzzer = 0;

  TriState led0 = null;
  TriState led1 = null;
  TriState led2 = null;
  TriState led3 = null;
  
  short eventmask = 0;
  short eventmaskmask = 0;
  
  public VMStatus(boolean start, boolean stop, short[] ip, short[] rgb, int buzzer, TriState led0, TriState led1, TriState led2, TriState led3, short eventmask, short eventmaskmask) {
    this.start = start;
    this.stop = stop;
    if(ip != null) {
      this.set_ip = true;
      this.ip = ip;
    }
    
    if(rgb != null) {
      this.set_rgb = true;
      this.rgb = rgb;
    }
    
    if(buzzer > 0) {
      this.set_buzzer = true;
      this.buzzer = buzzer;
    }
    
    this.led0 = led0;
    this.led1 = led1;
    this.led2 = led2;
    this.led3 = led3;
    
    this.eventmask = eventmask;
    this.eventmaskmask = eventmaskmask;
  }
  
  public VMStatus(String s) {
    String[] tokens = s.split("\\s");
    int i = 0;
    
    start = Boolean.parseBoolean(tokens[i++]);
    stop = Boolean.parseBoolean(tokens[i++]);

    set_ip = Boolean.parseBoolean(tokens[i++]);
    if(set_ip) {
      ip = new short[2];
      String iphex = tokens[i++];
      ip[0] = Short.parseShort(iphex.substring(0,2), 16);
      ip[1] = Short.parseShort(iphex.substring(2,4), 16);
    }

    set_rgb = Boolean.parseBoolean(tokens[i++]);
    if(set_rgb) {
      rgb = new short[3];
      
      String rgbhex = tokens[i++];
      rgb[0] = Short.parseShort(rgbhex.substring(0,2), 16);
      rgb[1] = Short.parseShort(rgbhex.substring(2,4), 16);
      rgb[2] = Short.parseShort(rgbhex.substring(4,6), 16);
    }
    
    set_buzzer = Boolean.parseBoolean(tokens[i++]);
    if(set_buzzer) {
      String buzzerHex = tokens[i++];
      short lower = Short.parseShort(buzzerHex.substring(2,4), 16);
      short higher = Short.parseShort(buzzerHex.substring(0,2), 16);
      buzzer = buzzer | lower;
      buzzer = buzzer | (higher << 8);
    }
    
    led0 = TriState.parse(tokens[i++]);
    led1 = TriState.parse(tokens[i++]);
    led2 = TriState.parse(tokens[i++]);
    led3 = TriState.parse(tokens[i++]);
    
    String eventHex = tokens[i++];
    eventmask = Short.parseShort(eventHex.substring(0,2), 16);
    eventmaskmask = Short.parseShort(eventHex.substring(2,4), 16);
  }
  
  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.print(start);
    pw.print(DELIM);
    pw.print(stop);
    pw.print(DELIM);
    
    pw.print(set_ip);
    pw.print(DELIM);
    
    if(set_ip) {
      pw.print(toHexByteString(ip[0]));
      pw.print(DELIM);
      pw.print(toHexByteString(ip[1]));
      pw.print(DELIM);
    }
    
    pw.print(set_rgb);
    pw.print(DELIM);
    
    if(set_rgb) {
      pw.print(toHexByteString(rgb[0]));
      pw.print(DELIM);
      pw.print(toHexByteString(rgb[1]));
      pw.print(DELIM);
      pw.print(toHexByteString(rgb[2]));
      pw.print(DELIM);
    }
    
    pw.print(set_buzzer);
    pw.print(DELIM);
    
    if(set_buzzer) {
      pw.print(toHexByteString(buzzer));
      pw.print(DELIM);
      pw.print(toHexByteString(buzzer, 1));
      pw.print(DELIM);
    }
    
    pw.print(led0);
    pw.print(DELIM);
    pw.print(led1);
    pw.print(DELIM);
    pw.print(led2);
    pw.print(DELIM);
    pw.print(led3);
    pw.print(DELIM);
    
    pw.print(toHexByteString(eventmask));
    pw.print(DELIM);
    pw.print(toHexByteString(eventmaskmask));
    pw.print(DELIM);
    
    return sw.toString();
  }
}