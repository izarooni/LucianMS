package com.lucianms.discord.handlers;

import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.CashShop;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 */
public class ReloadCSRequest extends DiscordRequest {

    @Override
    public void handle(MaplePacketReader reader) {
        CashShop.CashItemFactory.loadCommodities();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.ReloadCS.value);
        writer.writeLong(reader.readLong());
        DiscordSession.sendPacket(writer.getPacket());
    }
}
