package com.lucianms.nio;

import com.lucianms.BanManager;
import com.lucianms.LLoginMain;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.io.IOException;

@ChannelHandler.Sharable
public class InternalLoginCommunicationsHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalLoginCommunicationsHandler.class);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        Server.getToggles().put("server_online", false);

        LOGGER.info("Server is now closed");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Server.getToggles().put("server_online", true);

        LOGGER.info("Server is now open");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] packet = (byte[]) msg;
        MaplePacketReader reader = new MaplePacketReader(packet);
        byte header = reader.readByte();
        switch (header) {
            case 0: {
                final String content = reader.readMapleAsciiString();
                LLoginMain.getServerHandler().getChannels().forEach(ch -> ch.writeAndFlush((MaplePacketCreator.serverNotice(0, content))));
                break;
            }
            case 1: {
                String username = reader.readMapleAsciiString();
                if (BanManager.pardonUser(username)) {
                    LOGGER.info("Successfully unbanned user '{}'", username);
                } else {
                    LOGGER.info("Failed to find any account named '{}'", username);
                }
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
