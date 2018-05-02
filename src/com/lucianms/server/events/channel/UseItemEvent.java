package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ExpTable;
import net.PacketEvent;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 * @author izaroni
 */
public class UseItemEvent extends PacketEvent {

    private int itemId;

    private short slot;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(4);
        slot = slea.readShort();
        itemId = slea.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isAlive()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item toUse = player.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
            if (itemId == 2002031) { // [custom] exp ticket
                int needed = ExpTable.getExpNeededForLevel(player.getLevel());
                double scale = (needed / 100d);
                double gain;
                if (player.getLevel() <= 30) { // 18% gain
                    gain = scale * 18d;
                } else if (player.getLevel() <= 60) { // 12% gain
                    gain = scale * 12d;
                } else if (player.getLevel() <= 90) { // 8% gain
                    gain = scale * 8d;
                } else { // 5% gain
                    gain = scale * 5d;
                }
                player.gainExp((int) Math.floor(gain), true, true);
            } else if (itemId == 2022178 || itemId == 2022433 || itemId == 2050004) {
                player.dispelDebuffs();
                remove(getClient(), slot);
                return null;
            } else if (itemId == 2050001) {
                player.dispelDebuff(MapleDisease.DARKNESS);
                remove(getClient(), slot);
                return null;
            } else if (itemId == 2050002) {
                player.dispelDebuff(MapleDisease.WEAKEN);
                remove(getClient(), slot);
                return null;
            } else if (itemId == 2050003) {
                player.dispelDebuff(MapleDisease.SEAL);
                player.dispelDebuff(MapleDisease.CURSE);
                remove(getClient(), slot);
                return null;
            } else if (itemId == 2000039) {
                NPCScriptManager.start(getClient(), 9990248);
                remove(getClient(), slot);
                return null;
            }
            if (isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(player)) {
                    remove(getClient(), slot);
                }
                return null;
            }
            remove(getClient(), slot);
            ii.getItemEffect(toUse.getItemId()).applyTo(player);
            player.checkBerserk();
        }
        return null;
    }

    private void remove(MapleClient c, short slot) {
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.announce(MaplePacketCreator.enableActions());
    }

    private boolean isTownScroll(int itemId) {
        return itemId >= 2030000 && itemId < 2030021;
    }

    public int getItemId() {
        return itemId;
    }

    public short getSlot() {
        return slot;
    }
}
