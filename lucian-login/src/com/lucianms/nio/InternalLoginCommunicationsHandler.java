package com.lucianms.nio;

import com.lucianms.BanManager;
import com.lucianms.LLoginMain;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.Server;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.io.IOException;

@ChannelHandler.Sharable
public class InternalLoginCommunicationsHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalLoginCommunicationsHandler.class);
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        LOGGER.info("{} disconnected", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("{} connected", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] packet = (byte[]) msg;
        MaplePacketReader r = new MaplePacketReader(packet);
        byte header = r.readByte();
        switch (InterPacketOperation.values()[header]) {
            case Message: {
                final String content = r.readMapleAsciiString();
                LLoginMain.getServerHandler().getChannels().forEach(ch -> ch.writeAndFlush((MaplePacketCreator.serverNotice(0, content))));
                break;
            }
            case BanManager: {
                String username = r.readMapleAsciiString();
                if (BanManager.pardonUser(username)) {
                    LOGGER.info("Successfully unbanned user '{}'", username);
                } else {
                    LOGGER.info("Failed to find any account named '{}'", username);
                }
                break;
            }
            case ServerStatus: {
                channels.add(ctx.channel());
                boolean onlineStatus = r.readByte() != 0;
                Server.getToggles().put("server_online", onlineStatus);
                LOGGER.info("Server is now {}", (onlineStatus ? "online" : "offline"));

                MaplePacketWriter w = new MaplePacketWriter();
                w.write(0);
                ctx.channel().writeAndFlush(w.getPacket());
                break;
            }
            case VoteResult: {
                String username = r.readAsciiString(13);
                MaplePacketWriter w = new MaplePacketWriter();
                w.write(1);
                w.writeMapleString(username.trim());
                channels.writeAndFlush(w.getPacket());
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            return;
        }
        cause.printStackTrace();
    }
}
