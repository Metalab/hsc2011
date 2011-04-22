package org.metalab.ygor.serial.packet;

import java.io.PrintWriter;
import java.io.StringWriter;

public class VMStatus extends Payload {
  TriState running = null;
  TriState singlestep = null;
  boolean reset = false;

  boolean set_interrupt = false;
  boolean set_ip = false;
  short[] ip = null;
  
  boolean set_sp = false;
  short[] sp = null;
  
  boolean set_sfp = false;
  short[] sfp = null;

  boolean clear_error = false;
  boolean clear_suspend = false;

  public VMStatus(TriState running, TriState singlestep, boolean reset, boolean set_interrupt, short[] ip, short[] sp, short[] sfp, boolean clear_error, boolean clear_suspend) {
    this.running = running;
    this.singlestep = singlestep;
    this.reset = reset;

    if(ip != null) {
      if(set_interrupt) this.set_interrupt = true;
      else this.set_ip = true;
      this.ip = ip;
    }

    if(sp != null) {
      this.set_sp = true;
      this.sp = sp;
    }

    if(sfp != null) {
      this.set_sfp = true;
      this.sfp = sfp;
    }

    this.clear_error = clear_error;
    this.clear_suspend = clear_suspend;
  }

  public VMStatus(String s) {
    String[] tokens = s.split("\\s");
    int i = 0;

    running = TriState.parse(tokens[i++]);
    singlestep = TriState.parse(tokens[i++]);
    reset = Boolean.parseBoolean(tokens[i++]);

    set_interrupt = Boolean.parseBoolean(tokens[i++]);
    set_ip = Boolean.parseBoolean(tokens[i++]);
    if(set_interrupt || set_ip) {
      ip = new short[2];
      String ipHex = tokens[i++];
      ip[0] = Short.parseShort(ipHex.substring(0, 2), 16);
      ip[1] = Short.parseShort(ipHex.substring(2, 4), 16);
    }

    set_sp = Boolean.parseBoolean(tokens[i++]);
    if(set_sp) {
      sp = new short[2];
      String spHex = tokens[i++];
      sp[0] = Short.parseShort(spHex.substring(0, 2), 16);
      sp[1] = Short.parseShort(spHex.substring(2, 4), 16);
    }

    set_sfp = Boolean.parseBoolean(tokens[i++]);
    if(set_sfp) {
      sfp = new short[2];
      String sfpHex = tokens[i++];
      sfp[0] = Short.parseShort(sfpHex.substring(0, 2), 16);
      sfp[1] = Short.parseShort(sfpHex.substring(2, 4), 16);
    }

    clear_error = Boolean.parseBoolean(tokens[i++]);
    clear_suspend = Boolean.parseBoolean(tokens[i++]);
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.print(running); pw.print(DELIM);
    pw.print(singlestep); pw.print(DELIM);
    pw.print(reset); pw.print(DELIM);

    pw.print(set_interrupt); pw.print(DELIM);
    pw.print(set_ip); pw.print(DELIM);
    if(set_interrupt || set_ip) {
      pw.print(toHexByteString(ip[0]));
      pw.print(toHexByteString(ip[1]));
    }

    pw.print(set_sp); pw.print(DELIM);
    if(set_sp) {
      pw.print(toHexByteString(sp[0]));
      pw.print(toHexByteString(sp[1]));
    }

    pw.print(set_sfp); pw.print(DELIM);
    if(set_sfp) {
      pw.print(toHexByteString(sfp[0]));
      pw.print(toHexByteString(sfp[1]));
    }

    pw.print(clear_error); pw.print(DELIM);
    pw.print(clear_suspend); pw.print(DELIM);

    return sw.toString();
  }
}
