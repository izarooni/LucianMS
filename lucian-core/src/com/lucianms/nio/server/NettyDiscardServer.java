package com.lucianms.nio.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
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
    private Class<? extends ByteToMessageDecoder> decoder;
    private Class<? extends MessageToByteEncoder> encoder;

    public NettyDiscardServer(String address, int port, ChannelInboundHandlerAdapter serverInboundHandler, EventLoopGroup parentGroup, Class<? extends ByteToMessageDecoder> decoder, Class<? extends MessageToByteEncoder> encoder) {
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
                .option(ChannelOption.SO_BACKLOG, 100)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));

                        pipeline.addLast("decoder", decoder.getDeclaredConstructor().newInstance());
                        pipeline.addLast("encoder", encoder.getDeclaredConstructor().newInstance());

                        pipeline.addLast(serverInboundHandler);
                    }
                });

        // Bind and start to accept incoming connections.
        channelFuture = b.bind(address, port).sync();
    }

    @Override
    public void close() throws InterruptedException {
        channelFuture.channel().close();
        channelFuture.channel().closeFuture().sync();
        parentGroup.shutdownGracefully().sync();
    }
}
