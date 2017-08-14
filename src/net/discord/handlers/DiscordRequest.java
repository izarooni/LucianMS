package net.discord.handlers;

import tools.data.input.GenericLittleEndianAccessor;

/**
 * @author izarooni
 */
public abstract class DiscordRequest {

    public abstract void handle(GenericLittleEndianAccessor lea);
}
