package com.lucianms.nio.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author izarooni
 */
public class NettyDiscardClient implements AutoCloseable {

    private final NioEventLoopGroup parentGroup;
    private final ChannelInboundHandlerAdapter serverInboundHandler;
    private final String address;
    private final int port;
    private ByteToMessageDecoder decoder;
    private MessageToByteEncoder encoder;
    private ChannelFuture channelFuture;

    public NettyDiscardClient(String address, int port, NioEventLoopGroup parentGroup, ChannelInboundHandlerAdapter serverInboundHandler, ByteToMessageDecoder decoder, MessageToByteEncoder encoder) {
        this.address = address;
        this.port = port;
        this.parentGroup = parentGroup;
        this.serverInboundHandler = serverInboundHandler;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public void run() throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(parentGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("decoder", decoder)
                                .addLast("encoder", encoder)
                                .addLast(serverInboundHandler);
                    }
                });
        channelFuture = b.connect(address, port);
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    @Override
    public void close() throws Exception {
        channelFuture.channel().close();
        channelFuture.channel().closeFuture().sync();
        parentGroup.shutdownGracefully();
    }
}
