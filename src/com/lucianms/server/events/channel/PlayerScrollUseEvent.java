package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.SkillFactory;
import client.inventory.*;
import client.inventory.Equip.ScrollResult;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author izarooni
 * @author Matze
 * @author Frz
 */
public final class PlayerScrollUseEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerScrollUseEvent.class);

    private short slot;
    private short destination;
    private boolean whiteScroll;
    private boolean legendarySpirit;

    @Override
    public void exceptionCaught(Throwable t) {
        MapleCharacter player = getClient().getPlayer();
        Item scroll = player.getInventory(MapleInventoryType.USE).getItem(slot);
        Item equip = player.getInventory(destination < 0 ? MapleInventoryType.EQUIP : MapleInventoryType.EQUIPPED).getItem(destination);
        LOGGER.warn("Unable to use scroll {} on item {}",
                (scroll == null ? null : scroll.getItemId()),
                (equip == null ? null : equip.getItemId()), t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        reader.skip(4);
        slot = reader.readShort();
        destination = reader.readShort();
        whiteScroll = (byte) (reader.readShort() & 2) == 2;
        legendarySpirit = player.getSkillLevel(SkillFactory.getSkill(1003)) > 0 && destination >= 0;
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        Equip toScroll = (Equip) player.getInventory(legendarySpirit ? MapleInventoryType.EQUIP : MapleInventoryType.EQUIPPED).getItem(destination);
        byte oldLevel = toScroll.getLevel();
        byte oldSlots = toScroll.getUpgradeSlots();

        MapleInventory useInventory = player.getInventory(MapleInventoryType.USE);
        Item scroll = useInventory.getItem(slot);
        if (toScroll.getUpgradeSlots() < 1 && !isCleanSlate(scroll.getItemId())) {
            player.announce(MaplePacketCreator.getInventoryFull());
            return null;
        }

        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null) {
            if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
                player.announce(MaplePacketCreator.getInventoryFull());
                return null;
            }
        }
        if (!isChaosScroll(scroll.getItemId()) && !isCleanSlate(scroll.getItemId())) {
            if (!canScroll(scroll.getItemId(), toScroll.getItemId())) {
                return null;
            }
        }

        if (isCleanSlate(scroll.getItemId()) && !(toScroll.getLevel() + toScroll.getUpgradeSlots() < ii.getEquipStats(toScroll.getItemId()).get("tuc"))) {
            // upgrade slots can be over because of hammers
            return null;
        }

        Item WSItem = player.getInventory(MapleInventoryType.USE).findById(2340000);

        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll, player.isGM());
        ScrollResult scrollResult = (scrolled == null) ? ScrollResult.CURSE : Equip.ScrollResult.FAIL;
        if (scrolled != null && (scrolled.getLevel() > oldLevel || (isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() == oldSlots + 1))) {
            scrollResult = Equip.ScrollResult.SUCCESS;
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, scroll.getPosition(), (short) 1, false);
        if (whiteScroll && !isCleanSlate(scroll.getItemId())) {
            MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, WSItem.getPosition(), (short) 1, false, false);
        }

        final List<ModifyInventory> mods = new ArrayList<>();
        if (scrollResult == Equip.ScrollResult.CURSE) {
            mods.add(new ModifyInventory(3, toScroll));
            player.getInventory(destination < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
        } else {
            mods.add(new ModifyInventory(3, scrolled));
            mods.add(new ModifyInventory(0, scrolled));
        }
        player.announce(MaplePacketCreator.modifyInventory(true, mods));
        player.getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(player.getId(), scrollResult, legendarySpirit));
        if (destination < 0 && (scrollResult == Equip.ScrollResult.SUCCESS || scrollResult == Equip.ScrollResult.CURSE)) {
            player.equipChanged();
        }
        return null;
    }

    private static boolean isCleanSlate(int scrollId) {
        return scrollId > 2048999 && scrollId < 2049004;
    }

    private static boolean isChaosScroll(int scrollId) {
        return scrollId >= 2049100 && scrollId <= 2049103;
    }

    private static boolean canScroll(int scrollId, int itemId) {
        return (scrollId / 100) % 100 == (itemId / 10000) % 100;
    }
}
