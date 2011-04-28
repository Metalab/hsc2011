package org.metalab.ygor.serial.packet;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Status extends Payload {
  TriState set_rgb = TriState.no;
  short[] rgb = null;
  
  TriState set_buzzer = TriState.no;
  int buzzer = 0;

  TriState led0 = null;
  TriState led1 = null;
  TriState led2 = null;
  TriState led3 = null;
  
  short eventmask = 0;
  short eventmaskmask = 0;
  
  public Status(short[] rgb, int buzzer, TriState led0, TriState led1, TriState led2, TriState led3, short eventmask, short eventmaskmask) {
    if(rgb != null) {
      this.set_rgb = TriState.yes;
      this.rgb = rgb;
    }
    
    if(buzzer > 0) {
      this.set_buzzer = TriState.yes;
      this.buzzer = buzzer;
    }
    
    this.led0 = led0;
    this.led1 = led1;
    this.led2 = led2;
    this.led3 = led3;
    
    this.eventmask = eventmask;
    this.eventmaskmask = eventmaskmask;
  }
  
  public Status(String s) {
    String[] tokens = s.split("\\s");
    int i = 0;

    set_rgb = TriState.parse(tokens[i++]);
    if(set_rgb == TriState.yes) {
      rgb = new short[3];
      
      String rgbhex = tokens[i++];
      rgb[0] = Short.parseShort(rgbhex.substring(0,2), 16);
      rgb[1] = Short.parseShort(rgbhex.substring(2,4), 16);
      rgb[2] = Short.parseShort(rgbhex.substring(4,6), 16);
    }
    
    set_buzzer = TriState.parse(tokens[i++]);
    if(set_buzzer == TriState.yes) {
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
    
    eventmask = Short.parseShort(tokens[i++],16);
    eventmaskmask = Short.parseShort(tokens[i++],16);
  }
  
  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    
    pw.print(set_rgb);
    pw.print(DELIM);
    
    if(set_rgb == TriState.yes) {
      pw.print(toHexByteString(rgb[0],2));
      pw.print(DELIM);
      pw.print(toHexByteString(rgb[1],2));
      pw.print(DELIM);
      pw.print(toHexByteString(rgb[2],2));
      pw.print(DELIM);
    }
    
    pw.print(set_buzzer);
    pw.print(DELIM);
    
    if(set_buzzer == TriState.yes) {
      pw.print(toHexByteString(buzzer, 4));
      pw.print(DELIM);
      pw.print(toHexByteString(buzzer, 4));
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
    
    pw.print(toHexByteString(eventmask,2));
    pw.print(DELIM);
    pw.print(toHexByteString(eventmaskmask,2));
    pw.print(DELIM);
    
    return sw.toString();
  }
}
