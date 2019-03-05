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
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ii.getInventoryType(itemId);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(c, itemId);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ItemConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
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
                        short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
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
                short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
            }
        } else if (quantity == 1) {
            Item nEquip = ii.getEquipById(itemId);
            if (nEquip != null) {
                nEquip.setFlag(flag);
                nEquip.setExpiration(expiration);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                short newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return false;
                }
                c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip))));
            } else {
                LOGGER.warn("Invalid item {} from player '{}'", itemId, c.getPlayer().getName());
                c.announce(MaplePacketCreator.getInventoryFull());
                c.announce(MaplePacketCreator.getShowInventoryFull());
                return false;
            }
        } else {
            throw new RuntimeException("Trying to create equip with non-one quantity");
        }
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
                            Item eItem = (Item) i.next();
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
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        boolean allowZero = consume && ItemConstants.isRechargable(item.getItemId());
        c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
        if (item.getQuantity() == 0 && !allowZero) {
            c.announce(MaplePacketCreator.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(3, item))));
        } else {
            c.announce(MaplePacketCreator.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(1, item))));
        }
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
                        removeQuantity = 0;
                        break;
                    } else {
                        removeQuantity -= item.getQuantity();
                        removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
                    }
                }
            }
        }
        if (removeQuantity > 0) {
            throw new RuntimeException("[HACK] Not enough items available of Item:" + itemId + ", Quantity (After Quantity/Over Current Quantity): " + (quantity - removeQuantity) + "/" + quantity);
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

    public static void equip(MapleClient c, short src, short dst) {
        MapleCharacter player = c.getPlayer();
        Equip source = (Equip) player.getInventory(MapleInventoryType.EQUIP).getItem(src);
        if (source == null || !MapleItemInformationProvider.getInstance().canWearEquipment(player, source, dst)) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if ((((source.getItemId() >= 1902000 && source.getItemId() <= 1902002) || source.getItemId() == 1912000) && player.isCygnus()) || ((source.getItemId() >= 1902005 && source.getItemId() <= 1902007) || source.getItemId() == 1912005) && !player.isCygnus()) {// Adventurer taming equipment
            return;
        }
        boolean itemChanged = false;
        if (MapleItemInformationProvider.getInstance().isUntradeableOnEquip(source.getItemId())) {
            source.setFlag((byte) ItemConstants.UNTRADEABLE);
            itemChanged = true;
        }
        if (source.getRingId() > -1) {
            MapleRing ring = player.getRingById(source.getRingId());
            if (ring != null) {
                ring.equip();
            }
        }
        if (dst == -6) { // unequip the overall
            Item top = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -5);
            if (top != null && isOverall(top.getItemId())) {
                if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -5, player.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -5) {
            final Item bottom = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -6);
            if (bottom != null && isOverall(source.getItemId())) {
                if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -6, player.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -10) {// check if weapon is two-handed
            Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (weapon != null && MapleItemInformationProvider.getInstance().isTwoHanded(weapon.getItemId())) {
                if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -11, player.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        } else if (dst == -11) {
            Item shield = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if (shield != null && MapleItemInformationProvider.getInstance().isTwoHanded(source.getItemId())) {
                if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.announce(MaplePacketCreator.getInventoryFull());
                    c.announce(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                unequip(c, (byte) -10, player.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
        }
        if (dst == -18) {
            if (player.getMount() != null) {
                player.getMount().setItemId(source.getItemId());
            }
        }
        if (source.getItemId() == 1122017) {
            player.scheduleSpiritPendant();
        }
        //1112413, 1112414, 1112405 (Lilin's Ring)
        source = (Equip) player.getInventory(MapleInventoryType.EQUIP).getItem(src);
        Equip target = (Equip) player.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        player.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            player.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        final List<ModifyInventory> mods = new ArrayList<>();
        if (itemChanged) {
            mods.add(new ModifyInventory(3, source));
            mods.add(new ModifyInventory(0, source.copy()));//to prevent crashes
        }

        source.setPosition(dst);
        player.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            player.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (player.getBuffedValue(MapleBuffStat.BOOSTER) != null && isWeapon(source.getItemId())) {
            player.cancelBuffStats(MapleBuffStat.BOOSTER);
        }

        mods.add(new ModifyInventory(2, source, src));
        c.announce(MaplePacketCreator.modifyInventory(true, mods));
        player.equipChanged();

        for (CQuestData data : player.getCustomQuests().values()) {
            if (!data.isCompleted()) {
                CQuestItemRequirement toLoot = data.getToCollect();
                toLoot.incrementRequirement(source.getItemId(), -source.getQuantity());
                boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                /*
                If checkRequirements returns true, the quest is finished. If checked is also false, then
                this is check means the quest is finished. The quest completion notification should only
                happen once unless a progress variable drops below the requirement
                 */
                    data.announceCompletion(c);
                }
                if (!data.isSilentComplete()) {
                    CQuestItemRequirement.CQuestItem qItem = toLoot.get(source.getItemId());
                    if (qItem != null) {
                        String name = MapleItemInformationProvider.getInstance().getName(source.getItemId());
                        name = (name == null) ? "NO-NAME" : name; // hmmm
                        player.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, qItem.getProgress(), qItem.getRequirement())));
                    }
                }
            }
        }
    }

    public static void unequip(MapleClient c, short src, short dst) {
        MapleCharacter player = c.getPlayer();
        Equip source = (Equip) player.getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) player.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        if (dst < 0) {
            return;
        }
        if (source == null) {
            return;
        }
        if (target != null && src <= 0) {
            c.announce(MaplePacketCreator.getInventoryFull());
            return;
        }
        if (source.getItemId() == 1122017) {
            c.getPlayer().unequipPendantOfSpirit();
        }
        if (source.getRingId() > -1) {
            MapleRing ring = c.getPlayer().getRingById(source.getRingId());
            if (ring != null) {
                // could have been deleted manually
                ring.unequip();
            }
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        c.announce(MaplePacketCreator.modifyInventory(true, Collections.singletonList(new ModifyInventory(2, source, src))));
        c.getPlayer().equipChanged();

        for (CQuestData data : player.getCustomQuests().values()) {
            if (!data.isCompleted()) {
                CQuestItemRequirement toLoot = data.getToCollect();
                toLoot.incrementRequirement(source.getItemId(), source.getQuantity());
                boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                /*
                If checkRequirements returns true, the quest is finished. If checked is also false, then
                this is check means the quest is finished. The quest completion notification should only
                happen once unless a progress variable drops below the requirement
                 */
                    data.announceCompletion(c);
                }
                if (!data.isSilentComplete()) {
                    CQuestItemRequirement.CQuestItem qItem = toLoot.get(source.getItemId());
                    if (qItem != null) {
                        String name = MapleItemInformationProvider.getInstance().getName(source.getItemId());
                        name = (name == null) ? "NO-NAME" : name; // hmmm
                        player.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, qItem.getProgress(), qItem.getRequirement())));
                    }
                }
            }
        }
    }

    public static MapleMapItem drop(MapleClient c, MapleInventoryType type, short src, short quantity) {
        MapleCharacter player = c.getPlayer();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
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
        if (type == MapleInventoryType.EQUIPPED && itemId == 1122017) {
            player.unequipPendantOfSpirit();
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
        for (CQuestData data : player.getCustomQuests().values()) {
            if (!data.isCompleted()) {
                CQuestItemRequirement toLoot = data.getToCollect();
                toLoot.incrementRequirement(itemId, -quantity);
                boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                /*
                If checkRequirements returns true, the quest is finished. If checked is also false, then
                this is check means the quest is finished. The quest completion notification should only
                happen once unless a progress variable drops below the requirement
                 */
                    data.announceCompletion(c);
                }
                if (!data.isSilentComplete()) {
                    CQuestItemRequirement.CQuestItem qItem = toLoot.get(itemId);
                    if (qItem != null) {
                        String name = ii.getName(itemId);
                        name = (name == null) ? "NO-NAME" : name; // hmmm
                        player.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, qItem.getProgress(), qItem.getRequirement())));
                    }
                }
            }
        }
        Point dropPos = new Point(player.getPosition());
        if (quantity < source.getQuantity() && !ItemConstants.isRechargable(itemId)) {
            Item target = source.copy();
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
                player.equipChanged();
            }
            if (player.getMap().getEverlast()) {
                return player.getMap().spawnItemDrop(player, player, source, dropPos, true, false);
            } else {
                return player.getMap().spawnItemDrop(player, player, source, dropPos, true, true);
            }
        }
    }

    private static boolean isOverall(int itemId) {
        return itemId / 10000 == 105;
    }

    private static boolean isWeapon(int itemId) {
        return itemId >= 1302000 && itemId < 1492024;
    }
}
