package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.ItemFactory;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

/**
 * @author izarooni
 */
public class HiredMerchantEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
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
