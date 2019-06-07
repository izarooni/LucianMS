package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

/**
 * @author AngelSL
 * @author izarooni
 */
public class PlayerSummoningBagUseEvent extends PacketEvent {

    private short slot;
    private int itemID;

    @Override
    public void exceptionCaught(MaplePacketReader reader, Throwable t) {
        if (t instanceof NullPointerException) {
            getClient().getPlayer().sendMessage(5, "This item is currently broken.");
        }
        super.exceptionCaught(reader, t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        slot = reader.readShort();
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isAlive()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        Item toUse = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemID) {
            int[][] toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(itemID);
            MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, slot, (short) 1, false);
            if (toSpawn != null) {
                for (int[] toSpawnChild : toSpawn) {
                    if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
                        MapleMonster monster = MapleLifeFactory.getMonster(toSpawnChild[0]);
                        if (monster != null) {
                            Point position = player.getPosition().getLocation();
                            player.getMap().spawnMonsterOnGroudBelow(monster, position);
                        }
                    }
                }
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
