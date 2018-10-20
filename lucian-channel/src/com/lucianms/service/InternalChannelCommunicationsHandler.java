package com.lucianms.service;

import com.lucianms.nio.send.MaplePacketWriter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class InternalChannelCommunicationsHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalChannelCommunicationsHandler.class);

    private static byte[] sendMessage(String content) {
        MaplePacketWriter writer = new MaplePacketWriter(content.length() + 3);
        writer.write(0);
        writer.writeMapleString(content);
        return writer.getPacket();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        byte[] p = sendMessage("The server is now available.\r\nYou may now login.");
        ctx.channel().writeAndFlush(p);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    }
}
