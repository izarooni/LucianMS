package com.lucianms.discord;

import com.lucianms.discord.handlers.DiscordRequest;
import com.lucianms.discord.handlers.DiscordRequestManager;
import com.lucianms.nio.receive.MaplePacketReader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.LittleEndianReader;

/**
 * Not much needs to be done here.
 * <p>
 * Receive packet > handle > respond
 * </p>
 *
 * @author izarooni
 */
public class DiscordServer extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordServer.class);

    private EventLoopGroup bossGroup;
    private ChannelFuture channelFuture;

    DiscordServer(int port) throws Exception {
        bossGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(DiscordServer.this);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(port).sync();
    }

    public void close() throws InterruptedException {
        channelFuture.channel().close();
        channelFuture.channel().closeFuture().sync();
        bossGroup.shutdownGracefully().sync();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        DiscordSession.setSession(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            MaplePacketReader reader = new MaplePacketReader(bytes);
            byte header = reader.readByte();
            DiscordRequest request = DiscordRequestManager.getRequest(header);
            if (request != null) {
                try {
                    request.handle(reader);
                } catch (Throwable t) {
                    LOGGER.error("Failed to handle packet 0x{}", Integer.toHexString(header));
                    t.printStackTrace();
                }
            } else {
                LOGGER.info("Packet header not handler 0x{}", Integer.toHexString(header));
            }
        } else {
            LOGGER.info("Unhandled message type {}\r\n{}", msg.getClass(), msg.toString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
