package com.lucianms.nio.receive;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.MapleCustomEncryption;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import tools.MapleAESOFB;

import java.util.List;

/**
 * @author izarooni
 */
public class MaplePacketDecoder extends ByteToMessageDecoder {

    private static final AttributeKey<DecodeState> DECODE_KEY = AttributeKey.newInstance("DECODE_CLIENT.state");

    private class DecodeState {
        private int PacketLength = -1;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf b, List<Object> list) throws Exception {
        Channel channel = ctx.channel();
        MapleClient client = channel.attr(MapleClient.CLIENT_KEY).get();
        if (!channel.hasAttr(DECODE_KEY)) {
            channel.attr(DECODE_KEY).set(new DecodeState());
        } else {
            DecodeState decodeState = channel.attr(DECODE_KEY).get();

            byte[] t = new byte[b.readableBytes()];
            b.getBytes(b.readerIndex(), t);
            if (b.readableBytes() >= 4 && decodeState.PacketLength == -1) {
                int packetHeader = b.readInt();
                if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
                    client.getSession().close();
                    return;
                }
                decodeState.PacketLength = MapleAESOFB.getPacketLength(packetHeader);
            } else if (b.readableBytes() < 4 && decodeState.PacketLength == -1) {
                return;
            }
            if (b.readableBytes() >= decodeState.PacketLength) {
                byte[] decryptedPacket = new byte[decodeState.PacketLength];
                b.readBytes(decryptedPacket);
                decodeState.PacketLength = -1;
                client.getReceiveCrypto().crypt(decryptedPacket);
                MapleCustomEncryption.decryptData(decryptedPacket);
                list.add(decryptedPacket);
            }
        }
    }
}
