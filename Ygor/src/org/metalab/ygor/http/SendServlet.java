package org.metalab.ygor.http;

import java.io.OutputStream;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.metalab.ygor.YgorDaemon;
import org.metalab.ygor.YgorException;
import org.metalab.ygor.client.Outgoing;
import org.metalab.ygor.db.YgorRequest;
import org.metalab.ygor.serial.packet.Packet;
import org.metalab.ygor.serial.packet.Packet.PacketType;

public class SendServlet extends YgorServlet {
  private Outgoing outgoing;
  
  public SendServlet() {
    super("text/html");
    this.outgoing = YgorDaemon.client().getOutgoing();
  }

  protected void process(YgorRequest query, OutputStream out) {
    String src = "C01DC0FFEBEEFFFF";
    String dest = query.value("dest");
    short seqnum = Short.parseShort(query.value("seqnum"));
    PacketType type = findType(query.value("type").charAt(0));
    String payload;
    try {
      payload = new String(URLCodec.decodeUrl(query.value("payload").getBytes()));
    } catch (DecoderException e) {
      throw new YgorException("Unable to decode payload", e);
    }
    String handle = query.value("handle");

    outgoing.dispatch(new Packet(type,seqnum, src, dest, payload, handle));
  }

  private static PacketType findType(char typeChar) {
    if(typeChar == PacketType.PKTT_STATUS.commandLetter)
      return PacketType.PKTT_STATUS;
    else if(typeChar == PacketType.PKTT_READ.commandLetter)
      return PacketType.PKTT_READ;
    else if(typeChar == PacketType.PKTT_WRITE.commandLetter)
      return PacketType.PKTT_WRITE;
    else if(typeChar == PacketType.PKTT_VMSTATUS.commandLetter)
      return PacketType.PKTT_VMSTATUS;
     else 
      throw new YgorException("Illegal packet type: " + typeChar);
  }
}
