package com.lucianms.service;

import com.lucianms.nio.receive.DirectPacketDecoder;
import com.lucianms.nio.send.DirectPacketEncoder;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.nio.server.NettyDiscardClient;
import com.lucianms.scheduler.TaskExecutor;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class InternalChannelCommunicationsHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalChannelCommunicationsHandler.class);

    private final String address;
    private final int port;
    private final NettyDiscardClient client;

    public InternalChannelCommunicationsHandler(String address, int port) throws Exception {
        this.address = address;
        this.port = port;

        client = new NettyDiscardClient(address, port, new NioEventLoopGroup(), this, DirectPacketDecoder.class, DirectPacketEncoder.class);
        client.run();
        attemptConnection();
    }

    private void attemptConnection() {
        ChannelFuture connect = client.getBootstrap().connect(address, port);
        connect.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (!channelFuture.isSuccess()) {
                    LOGGER.info("Failed to connect to login server... Retrying in 5 seconds");
                    channelFuture.channel().eventLoop().schedule(() -> attemptConnection(), 5000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private static byte[] sendMessage(String content) {
        MaplePacketWriter writer = new MaplePacketWriter(content.length() + 3);
        writer.write(0);
        writer.writeMapleString(content);
        return writer.getPacket();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Disconnected from login server");
        attemptConnection();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Connected to login server");
        byte[] p = sendMessage("The server is now available.\r\nYou may login.");
        ctx.channel().writeAndFlush(p);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            return;
        }
        cause.printStackTrace();
    }
}
