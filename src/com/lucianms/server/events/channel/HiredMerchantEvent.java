package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.inventory.ItemFactory;
import net.PacketEvent;
import server.maps.MapleMapObjectType;
import tools.Database;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

/**
 * @author izarooni
 */
public class HiredMerchantEvent extends PacketEvent {

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (player.getMap().getMapObjectsInRange(player.getPosition(), 23000, Collections.singletonList(MapleMapObjectType.HIRED_MERCHANT)).isEmpty()
                && player.getMapId() > 910000000 && player.getMapId() < 910000023) {
            if (!player.hasMerchant()) {
                try (Connection con = getClient().getChannelServer().getConnection()) {
                    if (ItemFactory.MERCHANT.loadItems(con, player.getId(), false).isEmpty() && player.getMerchantMeso() == 0) {
                        getClient().announce(MaplePacketCreator.hiredMerchantBox());
                    } else {
                        getClient().announce(MaplePacketCreator.retrieveFirstMessage());
                    }
                } catch (SQLException e) {
                    getLogger().error("Unable to load items: {}", e.getMessage());
                }
            } else {
                player.sendMessage(1, "You already have a store open.");
            }
        } else {
            player.sendMessage(1, "You cannot open your hired merchant here.");
        }
        return null;
    }
}
