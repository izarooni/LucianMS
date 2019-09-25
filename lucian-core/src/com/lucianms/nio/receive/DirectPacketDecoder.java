package com.lucianms.nio.receive;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author izarooni
 */
public class DirectPacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf b, List<Object> list) throws Exception {
        byte[] bytes = new byte[b.readableBytes()];
        b.readBytes(bytes);
        list.add(bytes);
    }
}
