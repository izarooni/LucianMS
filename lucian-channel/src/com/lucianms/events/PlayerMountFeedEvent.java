package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ExpTable;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

/**
 * @author PurpleMadness
 * @author izarooni
 */
public class PlayerMountFeedEvent extends PacketEvent {

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(6);
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getInventory(MapleInventoryType.USE).findById(itemID) != null) {
            if (player.getMount() != null && player.getMount().getTiredness() > 0) {
                player.getMount().setTiredness(Math.max(player.getMount().getTiredness() - 30, 0));
                player.getMount().setExp(2 * player.getMount().getLevel() + 6 + player.getMount().getExp());
                int level = player.getMount().getLevel();
                boolean levelup = player.getMount().getExp() >= ExpTable.getMountExpNeededForLevel(level) && level < 31;
                if (levelup) {
                    player.getMount().setLevel(level + 1);
                }
                player.getMap().broadcastMessage(MaplePacketCreator.updateMount(player.getId(), player.getMount(), levelup));
                MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, false);
            }
        }
        return null;
    }
}