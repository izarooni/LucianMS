package com.lucianms.server;

import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.ItemFactory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import tools.MaplePacketCreator;
import tools.Pair;

import java.sql.*;
import java.util.*;

/**
 * @author izarooni
 */
public class MapleStorage extends EnumMap<MapleInventoryType, Set<Item>> {

    private static final byte INIT_SLOT_COUNT = 4;
    private ArrayList<Item> items = new ArrayList<>();
    private volatile boolean opened;
    private final int ID;
    private int money;
    private byte slotCount;

    private MapleStorage(int ID, byte slotCount, int money) {
        super(MapleInventoryType.class);
        this.ID = ID;
        this.slotCount = slotCount;
        this.money = money;
        for (MapleInventoryType type : MapleInventoryType.values()) {
            put(type, new HashSet<>());
        }
    }

    public static MapleStorage load(int id, int world) throws SQLException {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select * from storages where accountid = ? and world = ?")) {
                ps.setInt(1, id);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int storageID = rs.getInt("storageid");
                        byte slots = rs.getByte("slots");
                        int mesos = rs.getInt("meso");
                        MapleStorage storage = new MapleStorage(storageID, slots, mesos);
                        List<Pair<Item, MapleInventoryType>> items = ItemFactory.STORAGE.loadItems(con, id, false);
                        for (Pair<Item, MapleInventoryType> pair : items) {
                            storage.add(pair.right, pair.left);
                        }
                        items.clear();
                        return storage;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("insert into storages (accountid, world, slots, meso) values (?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, id);
                ps.setInt(2, world);
                ps.setInt(3, INIT_SLOT_COUNT);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new MapleStorage(rs.getInt(1), INIT_SLOT_COUNT, 0);
                    }
                    throw new RuntimeException("Failed to create a storage");
                }
            }
        }
    }

    public int countItems() {
        return items.size();
    }

    public synchronized Item get(MapleInventoryType type, short slot) {
        Set<Item> items = get(type);
        Iterator<Item> iterator = items.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (i++ == slot) {
                return item;
            }
        }
        return null;
    }

    public synchronized void add(MapleInventoryType type, Item item) {
        item.setPosition((short) items.size());
        items.add(item);
        get(type).add(item);

        items.sort(Comparator.comparingInt(i -> ItemConstants.getInventoryType(i.getItemId()).getType()));
    }

    public synchronized Item remove(MapleInventoryType type, short slot) {
        Set<Item> items = get(type);
        Iterator<Item> iterator = items.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (i++ == slot) {
                iterator.remove();
                return item;
            }
        }
        return null;
    }

    /**
     * Do not touch!
     *
     * <p>
     * This method is used in scripts
     * </p>
     *
     * @param c     User receiving the packet
     * @param npcID NPC Dialogue window speaker
     */
    public synchronized void sendStorage(MapleClient c, int npcID) {
        if (opened) return;
        opened = true;
        items.clear();
        values().forEach(l -> items.addAll(l));
        c.announce(MaplePacketCreator.getStorage(npcID, this));
    }

    public synchronized boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public int getID() {
        return ID;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        if (money < 0) {
            throw new IllegalArgumentException("Cannot deposit negative money: " + money);
        }
        this.money = money;
    }

    public byte getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(byte slotCount) {
        this.slotCount = (byte) Math.min(slotCount, 48);
    }

    public boolean increaseSlotCount(int size) {
        int lSlot = this.slotCount + size;
        if (lSlot <= 48) {
            this.slotCount = (byte) lSlot;
        }
        return false;
    }

    public void saveToDB(Connection con, int accID) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("update storages set slots = ?, meso = ? where storageid = ?")) {
            ps.setInt(1, slotCount);
            ps.setInt(2, money);
            ps.setInt(3, ID);
            ps.executeUpdate();
        }

        List<Pair<Item, MapleInventoryType>> itemsPair = new ArrayList<>();
        for (Entry<MapleInventoryType, Set<Item>> entry : entrySet()) {
            entry.getValue().forEach(item -> itemsPair.add(new Pair<>(item, entry.getKey())));
        }
        ItemFactory.STORAGE.saveItems(itemsPair, accID, con);
        itemsPair.clear();
    }
}
