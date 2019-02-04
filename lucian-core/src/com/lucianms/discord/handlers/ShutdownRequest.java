package com.lucianms.discord.handlers;

import com.lucianms.nio.receive.MaplePacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class ShutdownRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownRequest.class);

    @Override
    public void handle(MaplePacketReader reader) {
        LOGGER.info("Server shutdown requested");
        System.exit(0); // server shutdown hook pls
    }
}
