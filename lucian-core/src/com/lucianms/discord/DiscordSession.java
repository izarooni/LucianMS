package com.lucianms.discord;

import com.lucianms.nio.send.MaplePacketWriter;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Use basic encoding/decoding, socket should be local ONLY
 *
 * @author izarooni
 */
public class DiscordSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSession.class);
    private static final int ListeningPort = 8483;

    private static HikariDataSource hikari = Database.createDataSource("hikari-discord");
    private static DiscordServer discordServer = null;
    private static Channel session;

    private DiscordSession() {
    }

    public static Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public static DiscordServer getDiscordServer() {
        return discordServer;
    }

    public static void listen() {
        if (discordServer != null) {
            LOGGER.warn("Discord server is already listening");
            return;
        }
        try {
            discordServer = new DiscordServer(ListeningPort);
        } catch (Exception e) {
            LOGGER.warn("Unable to create Discord listener on port", ListeningPort, e);
        }
    }

    public static void sendPacket(byte[] packet) {
        if (discordServer == null) {
            LOGGER.error("Currently not connected to the server");
            return;
        }

        session.writeAndFlush(packet);
    }

    public static Channel getSession() {
        return session;
    }

    public static void setSession(Channel session) {
        DiscordSession.session = session;
    }

    public static void sendMessage(long channelID, String content) {
        MaplePacketWriter writer = new MaplePacketWriter();
        writer.write(Headers.MessageChannel.value);
        writer.writeLong(channelID);
        writer.writeMapleString(content);
        sendPacket(writer.getPacket());
    }
}
