package com.lucianms.discord.handlers;

import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import server.CashShop;
import tools.data.input.GenericLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 */
public class ReloadCSRequest extends DiscordRequest {

    @Override
    public void handle(GenericLittleEndianAccessor lea) {
        CashShop.CashItemFactory.loadCommodities();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.ReloadCS.value);
        writer.writeLong(lea.readLong());
        DiscordSession.sendPacket(writer.getPacket());
    }
}
