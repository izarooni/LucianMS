package net.discord.handlers;

import net.discord.DiscordSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.GenericLittleEndianAccessor;

/**
 * @author izarooni
 */
public class ShutdownRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownRequest.class);

    @Override
    public void handle(GenericLittleEndianAccessor lea) {
        LOGGER.info("Server shutdown requested");
        DiscordSession.getSession().closeNow();
        System.exit(0);
    }
}
