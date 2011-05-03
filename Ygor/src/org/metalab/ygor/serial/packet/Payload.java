package org.metalab.ygor.serial.packet;

public class Payload {
  public final static char DELIM = ' ';
  
  public enum TriState {
    yes('y'),
    no('n'),
    keep('z');
    
    private final char stateChar;
    
    TriState(char c) {
      stateChar = c;
    }
    
    public static TriState parse(String s) {
      s = s.trim();
      if(s.equalsIgnoreCase("y"))
        return TriState.yes;
      else if(s.equalsIgnoreCase("n"))
        return TriState.no;
      else if(s.equalsIgnoreCase("z"))
        return TriState.keep;
      else 
        return null;
    }
    
    public String toString() {
      return String.valueOf(stateChar);
    }
  }
  
  public final static String toHexByteString(int i, int byteIndex, int len) {
    return Integer.toHexString(0xff & (i >> (8 * byteIndex))).substring(0,len);
  }
  
  public final static String toHexByteString(int i, int len) {
    return Integer.toHexString(i).substring(0,len);
  }
}