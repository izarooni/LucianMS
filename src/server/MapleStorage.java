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
package server;

import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import net.server.Server;
import tools.Database;
import tools.MaplePacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Matze
 */
public class MapleStorage {

    private int id;
    private List<Item> items;
    private int meso;
    private byte slots;
    private Map<MapleInventoryType, List<Item>> typeItems = new HashMap<>();

    private MapleStorage(int id, byte slots, int meso) {
        this.id = id;
        this.slots = slots;
        this.items = new LinkedList<>();
        this.meso = meso;
    }

    private static MapleStorage create(Connection con, int id, int world) {
        try {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO storages (accountid, world, slots, meso) VALUES (?, ?, 4, 0)")) {
                ps.setInt(1, id);
                ps.setInt(2, world);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loadOrCreateFromDB(id, world);
    }

    public static MapleStorage loadOrCreateFromDB(int id, int world) {
        int storeId;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT storageid, slots, meso FROM storages WHERE accountid = ? AND world = ?")) {
                ps.setInt(1, id);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return create(con, id, world);
                    } else {
                        storeId = rs.getInt("storageid");
                        MapleStorage ret = new MapleStorage(storeId, (byte) rs.getInt("slots"), rs.getInt("meso"));
                        for (Pair<Item, MapleInventoryType> item : ItemFactory.STORAGE.loadItems(con, ret.id, false)) {
                            ret.items.add(item.getLeft());
                        }
                        return ret;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public byte getSlots() {
        return slots;
    }

    public boolean gainSlots(int slots) {
        slots += this.slots;

        if (slots <= 48) {
            this.slots = (byte) slots;
            return true;
        }

        return false;
    }

    public void setSlots(byte set) {
        this.slots = set;
    }

    public void saveToDB(Connection con) {
        try {
            try (PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?")) {
                ps.setInt(1, slots);
                ps.setInt(2, meso);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

            for (Item item : items) {
                itemsWithType.add(new Pair<>(item, MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId())));
            }

            ItemFactory.STORAGE.saveItems(itemsWithType, id, con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Item getItem(byte slot) {
        return items.get(slot);
    }

    public Item takeOut(byte slot) {
        Item ret = items.remove(slot);
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(ret.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
        return ret;
    }

    public void store(Item item) {
        items.add(item);
        MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    private List<Item> filterItems(MapleInventoryType type) {
        List<Item> ret = new LinkedList<>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Item item : items) {
            if (ii.getInventoryType(item.getItemId()) == type) {
                ret.add(item);
            }
        }
        return ret;
    }

    public byte getSlot(MapleInventoryType type, byte slot) {
        byte ret = 0;
        for (Item item : items) {
            if (item == typeItems.get(type).get(slot)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    public void sendStorage(MapleClient c, int npcId) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                if (ii.getInventoryType(o1.getItemId()).getType() < ii.getInventoryType(o2.getItemId()).getType()) {
                    return -1;
                } else if (ii.getInventoryType(o1.getItemId()) == ii.getInventoryType(o2.getItemId())) {
                    return 0;
                }
                return 1;
            }
        });
        for (MapleInventoryType type : MapleInventoryType.values()) {
            typeItems.put(type, new ArrayList<>(items));
        }
        c.announce(MaplePacketCreator.getStorage(npcId, slots, items, meso));
    }

    public void sendStored(MapleClient c, MapleInventoryType type) {
        c.announce(MaplePacketCreator.storeStorage(slots, type, typeItems.get(type)));
    }

    public void sendTakenOut(MapleClient c, MapleInventoryType type) {
        c.announce(MaplePacketCreator.takeOutStorage(slots, type, typeItems.get(type)));
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        if (meso < 0) {
            throw new RuntimeException();
        }
        this.meso = meso;
    }

    public void sendMeso(MapleClient c) {
        c.announce(MaplePacketCreator.mesoStorage(slots, meso));
    }

    public boolean isFull() {
        return items.size() >= slots;
    }

    public void close() {
        typeItems.clear();
    }
}
