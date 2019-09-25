package com.lucianms.nio.send;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;

/**
 * @author izarooni
 */
public class DirectPacketEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf b) throws Exception {
        byte[] bytes = (byte[]) o;
        b.writeBytes(ByteBuffer.wrap(bytes));
    }
}
