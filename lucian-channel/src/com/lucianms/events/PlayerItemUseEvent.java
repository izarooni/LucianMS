package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleDisease;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ExpTable;
import com.lucianms.constants.ItemConstants;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import tools.EntryLimits;
import tools.MaplePacketCreator;

import java.util.concurrent.TimeUnit;

/**
 * @author Matze
 * @author izaroni
 */
public class PlayerItemUseEvent extends PacketEvent {

    private int itemId;

    private short slot;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        slot = reader.readShort();
        itemId = reader.readInt();
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
            if (itemId == ItemConstants.ExpTicket) { // [custom] exp ticket
                EntryLimits.Entry entry = EntryLimits.getEntries(player.getId(), "exp_ticket");
                if (entry != null && entry.Entries >= 3) {
                    if (System.currentTimeMillis() - entry.LastEntry <= TimeUnit.DAYS.toMillis(1)) {
                        player.sendMessage(5, "You may only use 3 EXP tickets per day.");
                        return null;
                    }
                }
                EntryLimits.incrementEntry(player.getId(), "exp_ticket");
                int needed = ExpTable.getExpNeededForLevel(player.getLevel());
                double scale = (needed / 100d);
                double gain;
                if (player.getLevel() <= 30) { // 23% gain
                    gain = scale * 23d;
                } else if (player.getLevel() <= 60) { // 17% gain
                    gain = scale * 17d;
                } else if (player.getLevel() <= 90) { // 13% gain
                    gain = scale * 13d;
                } else { // 5% gain
                    gain = scale * 10d;
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
