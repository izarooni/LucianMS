package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ExpTable;
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
            if (player.getVehicle() != null && player.getVehicle().getTiredness() > 0) {
                player.getVehicle().setTiredness(Math.max(player.getVehicle().getTiredness() - 30, 0));
                player.getVehicle().setExp(2 * player.getVehicle().getLevel() + 6 + player.getVehicle().getExp());
                int level = player.getVehicle().getLevel();
                boolean levelup = player.getVehicle().getExp() >= ExpTable.getMountExpNeededForLevel(level) && level < 31;
                if (levelup) {
                    player.getVehicle().setLevel(level + 1);
                }
                player.getMap().broadcastMessage(MaplePacketCreator.updateMount(player.getId(), player.getVehicle(), levelup));
                MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, itemID, 1, true, false);
            }
        }
        return null;
    }
}