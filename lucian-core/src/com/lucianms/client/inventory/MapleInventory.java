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
package com.lucianms.client.inventory;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author Matze
 */
public class MapleInventory implements Iterable<Item> {

    public static final int MaxSlotCount = 96;

    private HashMap<Short, Item> inventory = new HashMap<>(100);
    private byte slotLimit;
    private MapleInventoryType type;
    private boolean checked = false;

    public MapleInventory(MapleInventoryType type, byte slotLimit) {
        this.type = type;
        this.slotLimit = slotLimit;
    }

    public static boolean checkSpot(MapleCharacter chr, Item item) {
        return !chr.getInventory(MapleInventoryType.getByType(item.getType())).isFull();
    }

    public static boolean checkSpots(MapleCharacter chr, List<Pair<Item, MapleInventoryType>> items) {
        int equipSlot = 0, useSlot = 0, setupSlot = 0, etcSlot = 0, cashSlot = 0;
        for (Pair<Item, MapleInventoryType> item : items) {
            if (item.getRight().getType() == MapleInventoryType.EQUIP.getType()) {
                equipSlot++;
            }
            if (item.getRight().getType() == MapleInventoryType.USE.getType()) {
                useSlot++;
            }
            if (item.getRight().getType() == MapleInventoryType.SETUP.getType()) {
                setupSlot++;
            }
            if (item.getRight().getType() == MapleInventoryType.ETC.getType()) {
                etcSlot++;
            }
            if (item.getRight().getType() == MapleInventoryType.CASH.getType()) {
                cashSlot++;
            }
        }

        if (chr.getInventory(MapleInventoryType.EQUIP).isFull(equipSlot - 1)) {
            return false;
        } else if (chr.getInventory(MapleInventoryType.USE).isFull(useSlot - 1)) {
            return false;
        } else if (chr.getInventory(MapleInventoryType.SETUP).isFull(setupSlot - 1)) {
            return false;
        } else if (chr.getInventory(MapleInventoryType.ETC).isFull(etcSlot - 1)) {
            return false;
        } else {
            return !chr.getInventory(MapleInventoryType.CASH).isFull(cashSlot - 1);
        }
    }

    public boolean isExtendableInventory() { // not sure about cash, basing this on the previous one.
        return !(type == MapleInventoryType.UNDEFINED || type == MapleInventoryType.EQUIPPED || type == MapleInventoryType.CASH);
    }

    public boolean isEquipInventory() {
        return type == MapleInventoryType.EQUIP || type == MapleInventoryType.EQUIPPED;
    }

    public byte getSlotLimit() {
        return slotLimit;
    }

    public void setSlotLimit(int newLimit) {
        slotLimit = (byte) Math.min(newLimit, MaxSlotCount);
    }

    public Item findById(int itemId) {
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public int countById(int itemId) {
        int possesed = 0;
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                possesed += item.getQuantity();
            }
        }
        return possesed;
    }

    public List<Item> listById(int itemId) {
        List<Item> ret = new ArrayList<>();
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                ret.add(item);
            }
        }
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    public Collection<Item> list() {
        return inventory.values();
    }

    public short addItem(Item item) {
        short slotId = getNextFreeSlot();
        if (slotId < 0 || item == null) {
            return -1;
        }
        inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public void addFromDB(Item item) {
        if (item.getPosition() < 0 && type != MapleInventoryType.EQUIPPED) {
            throw new RuntimeException("Adding item of negative slot value to non-equip inventory");
        }
        inventory.put(item.getPosition(), item);
    }

    public void move(short sSlot, short dSlot, short slotMax) {
        Item source = inventory.get(sSlot);
        Item target = inventory.get(dSlot);
        if (source == null) {
            return;
        }
        if (target == null) {
            source.setPosition(dSlot);
            inventory.put(dSlot, source);
            inventory.remove(sSlot);
        } else if (target.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId())) {
            if (type.getType() == MapleInventoryType.EQUIP.getType()) {
                swap(target, source);
            } else {
                if (source.getQuantity() + target.getQuantity() > slotMax) {
                    short rest = (short) ((source.getQuantity() + target.getQuantity()) - slotMax);
                    source.setQuantity(rest);
                    target.setQuantity(slotMax);
                } else {
                    target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
                    inventory.remove(sSlot);
                }
            }
        } else {
            swap(target, source);
        }
    }

    private void swap(Item source, Item target) {
        inventory.remove(source.getPosition());
        inventory.remove(target.getPosition());
        short swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        inventory.put(source.getPosition(), source);
        inventory.put(target.getPosition(), target);
    }

    public <T extends Item> T getItem(short slot) {
        return (T) inventory.get(slot);
    }

    public void removeItem(short slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(short slot, short quantity, boolean allowZero) {
        Item item = inventory.get(slot);
        if (item == null) {
            return;
        }
        if (!(item instanceof Equip)) {
            item.setQuantity((short) (item.getQuantity() - quantity));
            if (item.getQuantity() < 0) {
                item.setQuantity((short) 0);
            }
            if (item.getQuantity() == 0 && !allowZero) {
                removeSlot(slot);
            }
        } else {
            removeSlot(slot);
        }
    }

    public void removeSlot(short slot) {
        inventory.remove(slot);
    }

    public boolean isFull() {
        return inventory.size() >= slotLimit;
    }

    public boolean isFull(int margin) {
        return inventory.size() + margin >= slotLimit;
    }

    public short getNextFreeSlot() {
        if (isFull()) {
            return -1;
        }
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.keySet().contains(i)) {
                return i;
            }
        }
        return -1;
    }

    public short getNumFreeSlot() {
        if (isFull()) {
            return 0;
        }
        short free = 0;
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.keySet().contains(i)) {
                free++;
            }
        }
        return free;
    }

    public MapleInventoryType getType() {
        return type;
    }

    @Override
    public Iterator<Item> iterator() {
        return Collections.unmodifiableCollection(inventory.values()).iterator();
    }

    public Item findByCashId(int cashId) {
        boolean isRing = false;
        Equip equip = null;
        for (Item item : inventory.values()) {
            if (item.getType() == MapleInventoryType.EQUIP.getType()) {
                equip = (Equip) item;
                isRing = equip.getRingId() > -1;
            }
            if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId()) == cashId) {
                return item;
            }
        }
        return null;
    }


    public boolean checked() {
        return checked;
    }

    public void checked(boolean yes) {
        checked = yes;
    }

    public void updateItem(MapleClient client, Item item) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.INVENTORY_OPERATION.getValue());
        w.writeBoolean(true); // record update_time
        w.write(2);

        w.write(3);
        w.write(ItemConstants.getInventoryType(item.getItemId()).getType());
        w.writeShort(item.getPosition());

        w.write(0);
        w.write(ItemConstants.getInventoryType(item.getItemId()).getType());
        w.writeShort(item.getPosition());
        MaplePacketCreator.addItemInfo(w, item, true);

        w.write(2);

        client.announce(w.getPacket());
    }

    public ArrayList<Item> find(Predicate<Item> predicate) {
        ArrayList<Item> items = new ArrayList<>();
        Iterator<Item> iterator = iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (predicate.test(item)) {
                items.add(item);
            }
        }
        return items;
    }
}