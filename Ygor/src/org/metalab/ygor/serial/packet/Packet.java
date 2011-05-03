package org.metalab.ygor.serial.packet;

import java.util.HashMap;

import org.metalab.ygor.YgorException;
import org.metalab.ygor.db.YgorResult;
import org.metalab.ygor.util.ParameterMap;

public class Packet implements ParameterMap {
  public PacketType p_type = null;
  public short seqnum = -1;
  public String src = null;
  public String dest = null;
  public Object payload = null;
  public String payloadString = null;
  public String handle = null;
  
  public enum PacketType {
    /* initiated by buzzer, acked by basestation */

    PKTT_LOGIN('L'), PKTT_LOGIN_ACK('l'),

    PKTT_EVENT('E'), PKTT_EVENT_ACK('e'),

    /* initiated by basestation, acked by buzzer */

    PKTT_STATUS('S'), PKTT_STATUS_ACK('s'),

    PKTT_VMSTATUS('V'), PKTT_VMSTATUS_ACK('v'),

    PKTT_WRITE('W'), PKTT_WRITE_ACK('w'),

    PKTT_READ('R'), PKTT_READ_ACK('r'),

    PKTT_RESET('X'), PKTT_RESET_ACK('x');

    public final char commandLetter;

    PacketType(char commandLetter) {
      this.commandLetter = commandLetter;
    }

    boolean matches(char letter) {
      return this.commandLetter == letter;
    }
  };

  public Packet(PacketType p_type, short seqnum, String src, String dest,
      String payloadString, String handle) {
    this.p_type = p_type;
    this.seqnum = seqnum;
    this.handle = handle;
    if (src.length() == 16)
      this.src = src;
    else
      throw new IllegalArgumentException("Invalid source address: " + src);

    if (dest.length() == 16)
      this.dest = dest;
    else
      throw new IllegalArgumentException("Invalid destination address: " + dest);

    setPayload(payloadString);
  }

  public void setPayload(String payloadString) {
    if (payloadString == null) {
      this.payloadString = "";
      this.payload = null;
    } else {
      this.payloadString = payloadString;
      this.payload = payloadString; // null; //parsePayload(payloadString);
    }
  }

  public String toString() {
    // len = ptype(1) + seqnum(2) + src(16) + dest(16) + payloadLen(?) +
    // newline(1)
    int len = 35 + payloadString.length() + 1;
    String seqNumHex = Integer.toHexString(seqnum);
    if (seqNumHex.length() < 2)
      seqNumHex = '0' + Integer.toHexString(seqnum);

    StringBuilder sb = new StringBuilder(len);
    char delim = ' ';
    sb.append(p_type.commandLetter).append(delim).append(seqNumHex)
        .append(delim).append(src).append(delim).append(dest).append(delim);

    if (payload != null)
      sb.append(payload).append(delim);

    return sb.toString().trim();
  }

  private Object parsePayload(String payload) {
    switch (p_type) {
    case PKTT_LOGIN:
      return new IButton(payload);

    case PKTT_EVENT:
      return new DeviceEvent(payload);

    case PKTT_STATUS:
      return new StateChange(payload);
    default:
      throw new YgorException("Unknown packet type");
    }
  }

  public static Packet parsePacket(String s) throws YgorException {
    // FIXME comment bug workaround
    // s = s.substring(13);
    String[] tokens = s.split("\\s");
    char cl = tokens[0].charAt(0);
    PacketType p_type = null;
    for (PacketType t : PacketType.values()) {
      if (t.matches(cl)) {
        p_type = t;
        break;
      }
    }

    if (p_type == null)
      throw new YgorException("Unknown packet type:" + s);

    short seqnum = Short.parseShort(tokens[1], 16);
    String src = tokens[2];
    String dest = tokens[3];
    String payload = null;

    if (tokens.length > 4)
      payload = tokens[4];

    return new Packet(p_type, seqnum, src, dest, payload, null);
  }

  public HashMap<String, Object> getParameterMap() {
    HashMap<String, Object> paramMap = new HashMap<String, Object>();
    paramMap.put("src", this.src);
    paramMap.put("dest", this.dest);
    paramMap.put("seqnum", this.seqnum);
    paramMap.put("type", this.p_type.commandLetter);
    paramMap.put("date", System.currentTimeMillis());

    if (this.p_type == PacketType.PKTT_LOGIN) {
      paramMap.put("ibutton", payloadString);
    } else {
      paramMap.put("payload", payloadString);
    } 

    return paramMap;
  }

  public Packet createResponse(PacketType p_type, Payload payload) {
    String payloadString = null;
    if (payload != null)
      payloadString = payload.toString();
    return new Packet(p_type, seqnum, dest, src, payloadString, handle);
  }

  public static Packet createFromYgorResult(PacketType p_type, YgorResult result)
      {
    return new Packet(p_type, result.getShort("seqnum"),
        result.getString("src"), result.getString("dest"), null, result.getString("handle"));
  }
}
