package net.server.channel.handlers;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

/**
 * @author AngelSL
 * @author izarooni
 */
public class PlayerSummoningBagUseEvent extends PacketEvent {

    private short slot;
    private int itemID;

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
            MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, slot, (short) 1, false);
            int[][] toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(itemID);
            for (int[] toSpawnChild : toSpawn) {
                if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), player.getPosition());
                }
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
