package com.lucianms.nio.server;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.ReceivePacketManager;
import com.lucianms.nio.ReceivePacketState;
import com.lucianms.nio.receive.MaplePacketDecoder;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketEncoder;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.Server;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.Attribute;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.AESCipher;
import tools.MaplePacketCreator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@ChannelHandler.Sharable
public class MapleServerInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleServerInboundHandler.class);

    private final int port;
    private final ReceivePacketManager packetManager;
    private final NettyDiscardServer discardServer;
    private final ReceivePacketState packetState;
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public MapleServerInboundHandler(ReceivePacketState packetState, String address, int port, EventLoopGroup parentGroup) throws Exception {
        this.packetState = packetState;
        this.port = port;
        discardServer = new NettyDiscardServer(address, port, this, parentGroup, MaplePacketDecoder.class, MaplePacketEncoder.class);
        discardServer.run();
        packetManager = new ReceivePacketManager(packetState);
    }

    public NettyDiscardServer getDiscardServer() {
        return discardServer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        byte[] seqRecv = {70, 114, 122, 82};
        byte[] seqSend = {82, 48, 120, 115};
        seqRecv[3] = (byte) (Math.random() * 255);
        seqSend[3] = (byte) (Math.random() * 255);

        AESCipher sendCipher = new AESCipher(seqSend, (short) (0xFFFF - ServerConstants.VERSION));
        AESCipher recvCipher = new AESCipher(seqRecv, ServerConstants.VERSION);

        byte[] handshake = MaplePacketCreator.getHello(ServerConstants.VERSION, seqSend, seqRecv);
        ctx.channel().writeAndFlush(handshake);

        MapleClient client = new MapleClient(sendCipher, recvCipher, ctx.channel());
        int channelBasePort = Server.getConfig().getNumber("ChannelBasePort").intValue();
        if (port >= channelBasePort) {
            client.setChannel((port - channelBasePort) + 1);
        }
        ctx.channel().attr(MapleClient.CLIENT_KEY).set(client);

        TaskExecutor.createTask(client::sendPing, 1500);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        Attribute<MapleClient> attr = ctx.channel().attr(MapleClient.CLIENT_KEY);
        try {
            if (attr.get() != null) {
                MapleClient client = attr.get();
                client.disconnect();
            }
        } finally {
            attr.set(null);
            ctx.channel().close();
            ctx.channel().closeFuture().sync();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client == null) {
            return;
        }
        MapleCharacter player = client.getPlayer();
        if (player != null) {
            player.saveToDB();
        }
        client.sendPing();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client == null) {
            return;
        }
        MaplePacketReader reader = new MaplePacketReader((byte[]) msg);
        short header = reader.readShort();
        Class<? extends PacketEvent> event = packetManager.getEvent(header);
        if (event != null) {
            PacketEvent packetEvent = event.getDeclaredConstructor().newInstance();
            try {
                packetEvent.setClient(client);
                if (packetEvent.inValidState()) {
                    packetEvent.processInput(reader);
                    MapleCharacter player = client.getPlayer();
                    if (player != null) {
                        player.getGenericEvents().forEach(g -> g.onPacketEvent(packetEvent));
                    }
                    if (!packetEvent.isCanceled()) {
                        packetEvent.onPacket();
                    }
                }
            } catch (Throwable t) {
                if (!packetEvent.exceptionCaught(t)) {
                    try (FileWriter fw = new FileWriter("errors.log", true)) {
                        try (PrintWriter pw = new PrintWriter(fw)) {
                            t.printStackTrace(pw);
                        }
                    }
                }
            } finally {
                packetEvent.packetCompleted();
            }
        } else {
            LOGGER.info("No packet event for packet '0x{} ({})' {}\r\n{}", Integer.toHexString(header), header, packetState.name(), reader.toString());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            return;
        }
        try (FileWriter fw = new FileWriter("errors.log", true)) {
            try (PrintWriter pw = new PrintWriter(fw)) {
                cause.printStackTrace(pw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChannelGroup getChannels() {
        return channels;
    }
}
