package org.metalab.ygor.serial.packet;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.metalab.ygor.YgorException;

public class Packet {
  public PacketType p_type = null;
  public short seqnum = -1;
  public String src = null;
  public String dest = null;
  public Object payload = null;
  public String payloadHex;
	
	enum PacketType
	{
	  /* initiated by buzzer, acked by basestation */

	  PKTT_LOGIN('L'),
	  PKTT_LOGIN_ACK('l'),

	  PKTT_EVENT('E'),
	  PKTT_EVENT_ACK('e'),

	  /* initiated by basestation, acked by buzzer */

	  PKTT_STATUS('S'),
	  PKTT_STATUS_ACK('s'),

	  PKTT_WRITE('W'),
	  PKTT_WRITE_ACK('w'),

	  PKTT_READ('R'),
	  PKTT_READ_ACK('r'),

	  PKTT_RESET('X'),
	  PKTT_RESET_ACK('x');
	 
	  public final char commandLetter;
	  
	  PacketType(char commandLetter) {
	    this.commandLetter = commandLetter;
	  }
	  
	  boolean matches(char letter) {
	    return this.commandLetter == letter;
	  }
	};
	
	public Packet(PacketType p_type, ResultSet rs) throws SQLException {
	  this(p_type, rs.getShort(1), rs.getString(2), rs.getString(3), rs.getString(4));
	}
	
	public Packet(PacketType p_type, short seqnum, String src, String dest, String payloadHex) {
		this.p_type = p_type;
		this.seqnum = seqnum;
		
		if(src.length() == 16)
		  this.src = src;
		else
		  throw new IllegalArgumentException("Invalid source address: " + src);
		  
    if(dest.length() == 16)
      this.dest = dest;
    else
      throw new IllegalArgumentException("Invalid destination address: " + dest);
    
    if(payloadHex == null) {
      this.payloadHex = "";
      this.payload = null;
    }
    else {
      this.payloadHex = payloadHex;
      this.payload = parsePayload(payloadHex);
    }
	}

  public String toString() {
    // len = ptype(1) + seqnum(2) + src(16) + dest(16) + payloadLen(?) + newline(1)
    int len = 35 + payloadHex.length() + 1;
    
    StringBuilder sb = new StringBuilder(len);
    sb.append(p_type.commandLetter)
    .append(Integer.toHexString(seqnum))
    .append(src)
    .append(dest)
    .append(payload)
    .append('\n');

    return sb.toString();
  }

  private Object parsePayload(String payload) {
    switch (p_type) {
    case PKTT_LOGIN:
      return new Address(payload);
      
    case PKTT_EVENT:
      return new Event(payload);
    default:
      throw new YgorException("Unknown packet type");
    }
  }
  
	public static Packet parsePacket(String s) throws YgorException {
	  //FIXME comment bug workaround
	  //s = s.substring(13);
	  String[] tokens = s.split("\\s");
	  char cl = tokens[0].charAt(0);
	  PacketType p_type = null;
	  for (PacketType t : PacketType.values()) {
	    if(t.matches(cl)) {
	      p_type = t;
	      break;
	    }
	  }

	  if(p_type == null)
	    throw new YgorException("Unknown packet type:" + s);

	  short seqnum = Short.parseShort(tokens[1], 16);	  
	  String src = tokens[2];
	  String dest = tokens[3];
	  String payload = null;
	  
	  if(tokens.length > 4)
	    payload = tokens[4];
	  
	  return new Packet(p_type, seqnum, src, dest, payload);
	}
}
