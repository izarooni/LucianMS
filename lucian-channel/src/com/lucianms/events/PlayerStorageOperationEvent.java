package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStorage;
import tools.MaplePacketCreator;

/**
 * @author Matze
 * @author izarooni
 */
public class PlayerStorageOperationEvent extends PacketEvent {

    private static final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

    private int itemID;
    private int mesos;
    private short quantity;
    private short slot;
    private byte action;
    private byte type;

    public static byte[] getResultFailed(byte type) {
        final MaplePacketWriter w = new MaplePacketWriter(3);
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(type);
        if (type == 23) {
            w.write(0);
        }
        return w.getPacket();
    }

    public static byte[] getResultFailedMessage(String content) {
        MaplePacketWriter w = new MaplePacketWriter(3 + content.length());
        w.writeShort(SendOpcode.STORAGE.getValue());
        w.write(23);
        w.writeBoolean(true);
        w.writeMapleString(content);
        return w.getPacket();
    }

    @Override
    public boolean exceptionCaught(Throwable t) {
        getClient().announce(getResultFailed((byte) 23));
        return super.exceptionCaught(t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 4: // take
                type = reader.readByte();
                slot = reader.readByte();
                break;
            case 5: // put
                slot = reader.readShort();
                itemID = reader.readInt();
                quantity = reader.readShort();
                break;
            case 6: // sort items
            case 8: // close bank
                break;
            case 7: // withdraw & deposit
                mesos = reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.getStorage().isOpened()) {
            return null;
        }
        boolean debugger = player.isDebug();

        switch (action) {
            case 4:
                if (debugger) {
                    player.sendMessage("type {}, slot {}", type, slot);
                }
                OnItemRemoved(player);
                break;
            case 5:
                if (debugger) {
                    player.sendMessage("slot {}, itemID {}, quantity {}", slot, itemID, quantity);
                }
                OnItemPut(player);
                break;
            case 6:
                break;
            case 7:
                if (debugger) {
                    player.sendMessage("money {}", mesos);
                }
                OnSetMoney(player);
                break;
            case 8:
                OnClosed(player);
                break;
        }
        return null;
    }

    private void OnItemRemoved(MapleCharacter player) {
        MapleInventoryType iType = MapleInventoryType.values()[type];
        if (iType == MapleInventoryType.UNDEFINED) {
            player.announce(getResultFailed((byte) 23));
            return;
        }
        MapleStorage storage = player.getStorage();
        Item item = storage.get(iType, slot);
        if (item == null) {
            player.announce(getResultFailed((byte) 23));
            return;
        }
        if (iType != ItemConstants.getInventoryType(item.getItemId())) {
            player.announce(getResultFailed((byte) 23));
            return;
        }
        if (MapleInventoryManipulator.checkSpace(player.getClient(), item.getItemId(), item.getQuantity(), item.getOwner())) {
            if ((item = storage.remove(iType, slot)) != null) {
                MapleInventoryManipulator.addFromDrop(player.getClient(), item, true);
                player.announce(MaplePacketCreator.getStorageTakeItem(storage, iType));
            } else {
                player.announce(getResultFailed((byte) 23));
            }
        } else {
            player.announce(getResultFailed((byte) 10));
        }
    }

    private void OnItemPut(MapleCharacter player) {
        MapleStorage storage = player.getStorage();
        MapleInventoryType iType = ItemConstants.getInventoryType(itemID);
        if (iType == MapleInventoryType.UNDEFINED) {
            player.announce(getResultFailed((byte) 23));
            return;
        }
        MapleInventory inventory = player.getInventory(iType);
        Item item = inventory.getItem(slot);
        if (item == null || item.getItemId() != itemID || quantity < 1) {
            player.announce(getResultFailed((byte) 23));
            return;
        }
        if (storage.countItems() >= storage.getSlotCount()) {
            player.announce(getResultFailed((byte) 17));
            return;
        }
        Item duplicate = item.duplicate();
        if (!(duplicate instanceof Equip)) {
            duplicate.setQuantity(quantity);
        }
        MapleInventoryManipulator.removeFromSlot(player.getClient(), iType, slot, quantity, false);
        storage.add(iType, duplicate);
        player.announce(MaplePacketCreator.getStoragePutItem(storage, iType));
    }

    private void OnSetMoney(MapleCharacter player) {
        MapleStorage storage = player.getStorage();
        if (mesos < 0) { // deposit
            mesos = Math.abs(mesos);
            if (player.getMeso() >= mesos) {
                int localMoney = storage.getMoney() + mesos;
                if (localMoney > 0) { // overflow
                    player.gainMeso(-mesos, false);
                    storage.setMoney(localMoney);
                    player.announce(MaplePacketCreator.getStorageSetMoney(storage));
                } else {
                    player.announce(getResultFailedMessage("The bank is having a hard time holding all your money."));
                }
            } else {
                player.announce(getResultFailed((byte) 16));
            }
        } else if (mesos > 0) { // withdraw
            if (player.getMeso() + mesos > 0) {
                int localMoney = storage.getMoney() - mesos;
                if (localMoney >= 0) {
                    player.gainMeso(mesos, false);
                    storage.setMoney(localMoney);
                    player.announce(MaplePacketCreator.getStorageSetMoney(storage));
                } else {
                    player.announce(getResultFailedMessage("Your bank does not hold that much money."));
                }
            } else {
                player.announce(getResultFailedMessage("You would be holding too much money."));
            }
        }
    }

    private void OnClosed(MapleCharacter player) {
        MapleStorage storage = player.getStorage();
        storage.setOpened(false);
    }
}