package com.lucianms.nio.server;

import com.lucianms.nio.receive.MaplePacketDecoder;
import com.lucianms.nio.send.MaplePacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyDiscardServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyDiscardServer.class);

    private final int port;
    private ChannelFuture channelFuture;
    private MapleServerInboundHandler serverInboundHandler;
    private EventLoopGroup parentGroup, childGroup;

    NettyDiscardServer(int port, MapleServerInboundHandler serverInboundHandler, EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        this.port = port;
        this.serverInboundHandler = serverInboundHandler;
        this.parentGroup = parentGroup;
        this.childGroup = childGroup;

    }

    public void run() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.group(parentGroup, (childGroup == null) ? parentGroup : childGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("decoder", new MaplePacketDecoder())
                                .addLast("encoder", new MaplePacketEncoder())
                                .addLast(serverInboundHandler);
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 200)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(port).sync();
        LOGGER.info("Discard server on {}", port);
    }

    public void close() throws Exception {
        channelFuture.channel().close();
        channelFuture.channel().closeFuture().sync();
        parentGroup.shutdownGracefully().sync();
        if (childGroup != null) {
            childGroup.shutdownGracefully().sync();
        }
    }
}
