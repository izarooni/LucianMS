package com.lucianms.nio.send;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.MaplePacketManipulator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;

public class MaplePacketEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf b) throws Exception {
        Channel channel = ctx.channel();
        MapleClient client = channel.attr(MapleClient.CLIENT_KEY).get();

        byte[] dont_touch = (byte[]) o;
        byte[] unencrypted = new byte[dont_touch.length];
        System.arraycopy(dont_touch, 0, unencrypted, 0, dont_touch.length);

        if (client == null) {
            b.writeBytes(ByteBuffer.wrap(unencrypted));
            return;
        }

        byte[] encrypted = new byte[unencrypted.length + 4];
        byte[] header = client.getSendCrypto().getPacketHeader(unencrypted.length);
        MaplePacketManipulator.encryptData(unencrypted);

        client.getLock().lock();
        try {
            client.getSendCrypto().crypt(unencrypted);
            System.arraycopy(header, 0, encrypted, 0, header.length);
            System.arraycopy(unencrypted, 0, encrypted, 4, unencrypted.length);
            b.writeBytes(ByteBuffer.wrap(encrypted));
        } finally {
            client.getLock().unlock();
        }
    }
}
