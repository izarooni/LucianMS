package com.lucianms.discord.handlers;

import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public abstract class DiscordRequest {

    public abstract void handle(MaplePacketReader reader);
}
