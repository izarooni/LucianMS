/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.server;

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleRing;
import com.lucianms.client.inventory.*;
import com.lucianms.constants.ItemConstants;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
import com.lucianms.server.maps.MapleMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Functions;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Matze
 */
public class MapleInventoryManipulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleInventoryManipulator.class);

    private static void checkItemQuestProgress(MapleCharacter player, int itemID, short quantity) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        for (CQuestData data : player.getCustomQuests().values()) {
            if (!data.isCompleted()) {
                CQuestItemRequirement toLoot = data.getToCollect();
                toLoot.incrementRequirement(itemID, quantity);
                boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                /*
                If checkRequirements returns true, the quest is finished. If checked is also false, then
                this is check means the quest is finished. The quest completion notification should only
                happen once unless a progress variable drops below the requirement
                 */
                    data.announceCompletion(player.getClient());
                }
                if (!data.isSilentComplete()) {
                    CQuestItemRequirement.CQuestItem qItem = toLoot.get(itemID);
                    if (qItem != null) {
                        String name = ii.getName(itemID);
                        name = (name == null) ? "NO-NAME" : name; // hmmm
                        player.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, qItem.getProgress(), qItem.getRequirement())));
                    }
                }
            }
        }
    }

    public static boolean addById(MapleClient c, int itemId, short quantity) {
        return addById(c, itemId, quantity, null, -1, -1);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, long expiration) {
        return addById(c, itemId, quantity, null, -1, (byte) 0, expiration);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, int petid) {
        return addById(c, itemId, quantity, owner, petid, -1);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, int petid, long expiration) {
        return addById(c, itemId, quantity, owner, petid, (byte) 0, expiration);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, int petid, byte flag, long expiration) {
        MapleCharacter player = c.getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(itemId);

        if (type != MapleInventoryType.EQUIP) {
            short slotMax = ii.getSlotMax(c, itemId);
            List<Item> existing = player.getInventory(type).listById(itemId);
            if (!ItemConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                eItem.setExpiration(expiration);
                                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, eItem))));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0 || ItemConstants.isRechargable(itemId)) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        Item nItem = new Item(itemId, (short) 0, newQ, petid);
                        nItem.setFlag(flag);
                        nItem.setExpiration(expiration);
                        short newSlot = player.getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.announce(MaplePacketCreator.getInventoryFull());
                            c.announce(MaplePacketCreator.getShowInventoryFull());
                            return false;
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                        if ((ItemConstants.isRechargable(itemId)) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.announce(MaplePacketCreator.enableActions());
                        return false;
                    }
                }
            } else {
                Item nItem = new Item(itemId, (short) 0, quantity, petid);
                nItem.setFlag(flag);
                nItem.setExpiration(expiration);
                short newSlot = player.getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
            }
        } else {
            Item nEquip = ii.getEquipById(itemId);
            if (nEquip != null) {
                nEquip.setFlag(flag);
                nEquip.setExpiration(expiration);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                short newSlot = player.getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip))));
            } else {
                LOGGER.warn("Invalid item {} from player '{}'", itemId, player.getName());
                c.announce(MaplePacketCreator.getInventoryFull());
                c.announce(MaplePacketCreator.getShowInventoryFull());
                return false;
            }
        }
        checkItemQuestProgress(player, itemId, quantity);
        return true;
    }

    public static boolean addFromDrop(MapleClient c, Item item, boolean show) {
        MapleCharacter player = c.getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(item.getItemId());
        if (ii.isPickupRestricted(item.getItemId()) && player.getItemQuantity(item.getItemId(), true) > 0) {
            c.announce(MaplePacketCreator.getInventoryFull());
            c.announce(MaplePacketCreator.showItemUnavailable());
            return false;
        } else if (!checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
            c.announce(MaplePacketCreator.getInventoryFull());
            c.announce(MaplePacketCreator.getShowInventoryFull());
            return false;
        }
        for (CQuestData qData : player.getCustomQuests().values()) {
            if (!qData.isCompleted()) {
                var qitem = qData.getToCollect().get(item.getItemId());
                if (qitem != null && qitem.isUnique() && player.countItem(item.getItemId()) >= qitem.getRequirement()) {
                    c.announce(MaplePacketCreator.showItemUnavailable());
                    return false;
                }
            }
        }
        short quantity = item.getQuantity();
        if (type != MapleInventoryType.EQUIP) {
            short slotMax = ii.getSlotMax(c, item.getItemId());
            List<Item> existing = player.getInventory(type).listById(item.getItemId());

            if (!existing.isEmpty()) {
                if (item.getItemId() >= 4011009 && item.getItemId() <= 4011009 + 6) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.showItemUnavailable());
                    return false;
                }
            }

            if (!ItemConstants.isRechargable(item.getItemId())) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner())) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, eItem))));
                            }
                        } else {
                            break;
                        }
                    }
                }
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    Item nItem = new Item(item.getItemId(), (short) 0, newQ);
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setFlag(item.getFlag());
                    short newSlot = player.getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                }
            } else {
                Item nItem = new Item(item.getItemId(), (short) 0, quantity);
                short newSlot = player.getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                c.announce(MaplePacketCreator.enableActions());
            }
        } else if (quantity == 1) {
            short newSlot = player.getInventory(type).addItem(item);
            if (newSlot == -1) {
                c.announce(MaplePacketCreator.getInventoryFull());
                c.announce(MaplePacketCreator.getShowInventoryFull());
                return false;
            }
            c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, item))));
        } else {
            return false;
        }
        if (show) {
            c.announce(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        checkItemQuestProgress(player, item.getItemId(), item.getQuantity());
        return true;
    }

    public static boolean checkSpace(MapleClient c, int itemid, int quantity, String owner) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(itemid);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemid);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!ItemConstants.isRechargable(itemid)) {
                if (existing.size() > 0) // first update all existing slots to slotMax
                {
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else if (ItemConstants.isRechargable(itemid)) {
                numSlotsNeeded = 1;
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static void removeFromSlot(MapleClient c, MapleInventoryType type, short slot, short quantity, boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static void removeFromSlot(MapleClient c, MapleInventoryType type, short slot, short quantity, boolean fromDrop, boolean consume) {
        MapleCharacter player = c.getPlayer();
        Item item = player.getInventory(type).getItem(slot);
        boolean allowZero = consume && ItemConstants.isRechargable(item.getItemId());

        player.getInventory(type).removeItem(slot, quantity, allowZero);
        if (item instanceof Equip || item.getQuantity() == 0 && !allowZero) {
            c.announce(MaplePacketCreator.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(3, item))));
        } else {
            c.announce(MaplePacketCreator.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(1, item))));
        }
        checkItemQuestProgress(player, item.getItemId(), quantity);
    }

    public static void removeById(MapleClient c, MapleInventoryType type, int itemId, int quantity, boolean fromDrop, boolean consume) {
        int removeQuantity = quantity;
        MapleInventory inv = c.getPlayer().getInventory(type);
        int slotLimit = type == MapleInventoryType.EQUIPPED ? 128 : inv.getSlotLimit();

        for (short i = 0; i <= slotLimit; i++) {
            Item item = inv.getItem((short) (type == MapleInventoryType.EQUIPPED ? -i : i));
            if (item != null) {
                if (item.getItemId() == itemId || item.getCashId() == itemId) {
                    if (removeQuantity <= item.getQuantity()) {
                        removeFromSlot(c, type, item.getPosition(), (short) removeQuantity, fromDrop, consume);
                        break;
                    } else {
                        removeQuantity -= item.getQuantity();
                        removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
                    }
                }
            }
        }
    }

    public static void move(MapleClient c, MapleInventoryType type, short src, short dst) {
        MapleCharacter player = c.getPlayer();
        if (src < 0 || dst < 0) {
            return;
        }
        if (dst > player.getInventory(type).getSlotLimit()) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item source = player.getInventory(type).getItem(src);
        Item initialTarget = player.getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        short oldsrcQ = source.getQuantity();
        short slotMax = ii.getSlotMax(c, source.getItemId());
        player.getInventory(type).move(src, dst, slotMax);
        final List<ModifyInventory> mods = new ArrayList<>();
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null && initialTarget.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId())) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                mods.add(new ModifyInventory(1, source));
                mods.add(new ModifyInventory(1, initialTarget));
            } else {
                mods.add(new ModifyInventory(3, source));
                mods.add(new ModifyInventory(1, initialTarget));
            }
        } else {
            mods.add(new ModifyInventory(2, source, src));
        }
        c.announce(MaplePacketCreator.modifyInventory(true, mods));
    }

    public static void equip(MapleClient c, short srcPosition, short dstPosition) {
        MapleCharacter player = c.getPlayer();
        MapleInventory srcInventory = player.getInventory(MapleInventoryType.EQUIP);
        MapleInventory dstInventory = player.getInventory(MapleInventoryType.EQUIPPED);

        Equip srcItem = srcInventory.getItem(srcPosition);
        Equip dstItem = dstInventory.getItem(dstPosition);

        if (srcItem == null || !MapleItemInformationProvider.getInstance().canWearEquipment(player, srcItem, dstPosition)) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if ((((srcItem.getItemId() >= 1902000 && srcItem.getItemId() <= 1902002) || srcItem.getItemId() == 1912000) && player.isCygnus()) || ((srcItem.getItemId() >= 1902005 && srcItem.getItemId() <= 1902007) || srcItem.getItemId() == 1912005) && !player.isCygnus()) {// Adventurer taming equipment
            return;
        }
        boolean itemChanged = false;
        if (MapleItemInformationProvider.getInstance().isUntradeableOnEquip(srcItem.getItemId())) {
            srcItem.setFlag((byte) ItemConstants.UNTRADEABLE);
            itemChanged = true;
        }

        MapleRing ring = null;
        if (srcItem.getRingId() > -1) {
            ring = player.getRingById(srcItem.getRingId());
            Functions.requireNotNull(ring, r -> r.setEquipped(true));
        }
        //region un-equip equips due to equip overrides
        switch (dstPosition) {
            case -6: {  // unequip the overall
                Item top = dstInventory.getItem((short) -5);
                if (top != null && ItemConstants.isOverall(top.getItemId())) {
                    if (srcInventory.isFull()) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, srcInventory.getNextFreeSlot());
                }
                break;
            }
            case -5: {
                final Item bottom = dstInventory.getItem((short) -6);
                if (bottom != null && ItemConstants.isOverall(srcItem.getItemId())) {
                    if (srcInventory.isFull()) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, srcInventory.getNextFreeSlot());
                }
                break;
            }
            case -10: { // check if weapon is two-handed
                Item weapon = dstInventory.getItem((short) -11);
                if (weapon != null && MapleItemInformationProvider.getInstance().isTwoHanded(weapon.getItemId())) {
                    if (srcInventory.isFull()) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, srcInventory.getNextFreeSlot());
                }
                break;
            }
            case -11: {
                Item shield = dstInventory.getItem((short) -10);
                if (shield != null && MapleItemInformationProvider.getInstance().isTwoHanded(srcItem.getItemId())) {
                    if (srcInventory.isFull()) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, srcInventory.getNextFreeSlot());
                }
                break;
            }
            case -18: {
                if (player.getMount() != null) {
                    player.getMount().setItemId(srcItem.getItemId());
                }
                break;
            }
        }
        //endregion
        if (srcItem.getItemId() == ItemConstants.SpiritPendant) {
            player.setSpiritPendantModifier((byte) 0);
        }
        // remove from inventory
        srcInventory.removeSlot(srcPosition);
        if (dstItem != null) {
            // remove from equips -- for replacement
            dstInventory.removeSlot(dstPosition);
        }
        // move from inventory to equipped
        srcItem.setPosition(dstPosition);
        dstInventory.addFromDB(srcItem);
        if (dstItem != null) {
            // move replaced item to inventory
            dstItem.setPosition(srcPosition);
            srcInventory.addFromDB(dstItem);
        }
        if (player.getBuffedValue(MapleBuffStat.BOOSTER) != null && ItemConstants.isWeapon(srcItem.getItemId())) {
            player.cancelBuffStats(MapleBuffStat.BOOSTER);
        }

        final List<ModifyInventory> mods = new ArrayList<>();
        if (itemChanged) {
            mods.add(new ModifyInventory(3, srcItem));
            mods.add(new ModifyInventory(0, srcItem.duplicate()));
        }
        mods.add(new ModifyInventory(2, srcItem, srcPosition));
        c.announce(MaplePacketCreator.modifyInventory(true, mods));
        player.equipChanged(ring != null);
        if (ring != null) {
            byte[] packet = MaplePacketCreator.getPlayerModified(player,
                    (ItemConstants.isCoupleEquip(ring.getItemId()) ? ring : null),
                    (ItemConstants.isFriendshipEquip(ring.getItemId()) ? ring : null));
            player.getMap().sendPacketCheckHiddenExclude(player, packet);
        }
    }

    public static void unequip(MapleClient c, short src, short dst) {
        MapleCharacter player = c.getPlayer();

        MapleInventory srcInventory = player.getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory dstInventory = player.getInventory(MapleInventoryType.EQUIP);

        Equip srcItem = srcInventory.getItem(src);
        Equip dstItem = dstInventory.getItem(dst);
        if (dst < 0) return;
        if (srcItem == null) return;
        if (dstItem != null && src <= 0) {
            c.announce(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (srcItem.getItemId() == ItemConstants.SpiritPendant) {
            player.setSpiritPendantModifier((byte) 0);
        }
        if (srcItem.getRingId() > -1) {
            MapleRing ring = player.getRingById(srcItem.getRingId());
            Functions.requireNotNull(ring, r -> r.setEquipped(false));
            if (ring == null) {
                srcItem.setRingId(-1);
            }
        }

        srcInventory.removeSlot(src); // remove from equips
        if (dstItem != null) {
            dstInventory.removeSlot(dst);
        }
        srcItem.setPosition(dst); // move from equips to inventory
        dstInventory.addFromDB(srcItem);
        if (dstItem != null) {
            dstItem.setPosition(src);
            srcInventory.addFromDB(dstItem);
        }
        c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(2, srcItem, src))));
        player.equipChanged(true);
    }

    public static MapleMapItem drop(MapleClient c, MapleInventoryType type, short src, short quantity) {
        MapleCharacter player = c.getPlayer();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        Item source = player.getInventory(type).getItem(src);

        if (player.getTrade() != null || player.getMiniGame() != null || source == null) { //Only check needed would prob be merchants (to see if the player is in one)
            return null;
        }
        int itemId = source.getItemId();
        if (itemId >= 5000000 && itemId <= 5002000) {
            return null;
        }
        if (type == MapleInventoryType.EQUIPPED && itemId == ItemConstants.SpiritPendant) {
            player.setSpiritPendantModifier((byte) 0);
        }
        if (player.getItemEffect() == itemId && source.getQuantity() == 1) {
            player.setItemEffect(0);
            player.getMap().broadcastMessage(MaplePacketCreator.itemEffect(player.getId(), 0));
        } else if (itemId == 5370000 || itemId == 5370001) {
            if (player.getItemQuantity(itemId, false) == 1) {
                player.setChalkboard(null);
            }
        }
        if ((!ItemConstants.isRechargable(itemId) && player.getItemQuantity(itemId, true) < quantity) || quantity < 0) {
            return null;
        }
        checkItemQuestProgress(player, itemId, (short) -quantity);
        Point dropPos = new Point(player.getPosition());
        if (quantity < source.getQuantity() && !ItemConstants.isRechargable(itemId)) {
            Item target = source.duplicate();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, source))));
            boolean weddingRing = source.getItemId() == 1112803 || source.getItemId() == 1112806 || source.getItemId() == 1112807 || source.getItemId() == 1112809;
            if (weddingRing) {
                return player.getMap().disappearingItemDrop(player, player, target, dropPos);
            } else if (player.getMap().getEverlast()) {
                return player.getMap().spawnItemDrop(player, player, target, dropPos, true, false);
            } else {
                return player.getMap().spawnItemDrop(player, player, target, dropPos, true, true);
            }
        } else {
            player.getInventory(type).removeSlot(src);
            c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(3, source))));
            if (src < 0) {
                player.equipChanged(true);
            }
            if (player.getMap().getEverlast()) {
                return player.getMap().spawnItemDrop(player, player, source, dropPos, true, false);
            } else {
                return player.getMap().spawnItemDrop(player, player, source, dropPos, true, true);
            }
        }
    }

}
