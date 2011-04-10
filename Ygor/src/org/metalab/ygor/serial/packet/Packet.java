package org.metalab.ygor.serial.packet;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

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
    
    setPayload(payloadHex);
	}
	
	public void setPayload(String payloadHex) {
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
      return new IButton(payload);
      
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
	
  public HashMap<String, Object> createParameterMap() {
    HashMap<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("src", this.src);
    paramMap.put("dest", this.dest);
    paramMap.put("seqnum", this.seqnum);
    paramMap.put("date", now());

    if(this.payload instanceof IButton)
      paramMap.put("ibutton", ((IButton) this.payload).hex);
    else if(this.payload instanceof Event) {
      Event e = (Event) this.payload;
      paramMap.put("reserved", e.reserved);
      paramMap.put("eventmask", e.eventmask);
    }

    return paramMap;
  }
  

  private static Date now() {
    return new Date(System.currentTimeMillis());
  }
  
	public Packet createResponse(PacketType p_type, Payload payload) {
	  String payloadString = null;
	  if(payload != null)
	    payloadString = payload.toString();
	  return new Packet(p_type, seqnum, src, dest, payloadString);
	}
	
	public static Packet createFromResultSet(PacketType p_type, ResultSet rs) throws SQLException {
	    return new Packet(p_type, rs.getShort("seqnum"), rs.getString("src"), rs.getString("dest"), null);
	}
}
