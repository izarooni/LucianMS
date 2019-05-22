package com.lucianms.discord.handlers;

import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.CashShop;

/**
 * @author izarooni
 */
public class ReloadCSRequest extends DiscordRequest {

    @Override
    public void handle(MaplePacketReader reader) {
        CashShop.CashItemFactory.loadCommodities();

        MaplePacketWriter w = new MaplePacketWriter();
        w.write(Headers.ReloadCS.value);
        w.writeLong(reader.readLong());
        DiscordConnection.sendPacket(w.getPacket());
    }
}
