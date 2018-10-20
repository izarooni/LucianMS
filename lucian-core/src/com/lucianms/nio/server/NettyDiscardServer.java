package com.lucianms.nio.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class NettyDiscardServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyDiscardServer.class);

    private final String address;
    private final int port;
    private ChannelFuture channelFuture;
    private ChannelInboundHandlerAdapter serverInboundHandler;
    private EventLoopGroup parentGroup;
    private ByteToMessageDecoder decoder;
    private MessageToByteEncoder encoder;

    public NettyDiscardServer(String address, int port, ChannelInboundHandlerAdapter serverInboundHandler, EventLoopGroup parentGroup, ByteToMessageDecoder decoder, MessageToByteEncoder encoder) {
        this.address = address;
        this.port = port;
        this.serverInboundHandler = serverInboundHandler;
        this.parentGroup = parentGroup;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public void run() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.group(parentGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("decoder", decoder)
                                .addLast("encoder", encoder)
                                .addLast(serverInboundHandler);
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 200)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(address, port).sync();
        LOGGER.info("Discard server on {}:{}", address, port);
    }

    @Override
    public void close() throws Exception {
        channelFuture.channel().close();
        channelFuture.channel().closeFuture().sync();
        parentGroup.shutdownGracefully().sync();
    }
}
