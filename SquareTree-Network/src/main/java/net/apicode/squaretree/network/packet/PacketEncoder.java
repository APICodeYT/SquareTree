package net.apicode.squaretree.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.apicode.squaretree.network.codec.DataSerializer;
import net.apicode.squaretree.network.codec.converter.NodeIdConverter;
import net.apicode.squaretree.network.util.ByteArrayEncryption;
import net.apicode.squaretree.network.util.NodeId;
import net.apicode.squaretree.network.util.SecurityInfo;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

  private final ProtocolManager protocolManager;
  private final SecurityInfo securityInfo;

  public PacketEncoder(ProtocolManager protocolManager, SecurityInfo securityInfo) {
    this.protocolManager = protocolManager;
    this.securityInfo = securityInfo;
  }

  /*
   * 0 packetid
   * 1 packet type
   * 2 container type
   * 3 sender
   * 4 target
   * 5 data
   * 6 bool (has response)
   *!7 response (nullable)
   * */

  @Override
  protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf)
      throws Exception {

    NodeIdConverter nodeIdConverter = new NodeIdConverter();

    String packetId = packet.getPacketId();
    int packetType = protocolManager.getPacketType(packet.getClass());
    int containerType = packet.getContainerType().getId();
    NodeId sender = packet.getSenderNode();
    NodeId target = packet.getTargetNode();

    boolean hasResponse = packet.getResponse() != null && packet.getContainerType() == PacketType.RESPONSE;

    DataSerializer packetContainer = new DataSerializer();
    packetContainer.writeString(packetId); //0
    packetContainer.writeInt(packetType); //1
    packetContainer.writeInt(containerType); //2
    packetContainer.writeData(sender, nodeIdConverter); //3
    packetContainer.writeData(target, nodeIdConverter); //4

    packetContainer.writeContainer(packet); //5
    packetContainer.writeBoolean(hasResponse); //6
    packet.getResponse().serialize(packetContainer);
    
    byte[] array;
    if(securityInfo.isCryptoModeAvailable()) {
      array = ByteArrayEncryption.xor(packetContainer.array(), securityInfo.getCryptoKey());
    } else {
      array = packetContainer.array();
    }
    byteBuf.writeInt(array.length);
    byteBuf.writeBytes(array);



  }
}
