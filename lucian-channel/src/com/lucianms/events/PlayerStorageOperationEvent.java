package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStorage;
import com.lucianms.server.Server;
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
    private byte slot;
    private byte action;
    private byte type;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 4:
                type = reader.readByte();
                slot = reader.readByte();
                break;
            case 5:
                slot = (byte) reader.readShort(); // huh
                itemID = reader.readInt();
                quantity = reader.readShort();
                break;
            case 7:
                mesos = reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleStorage storage = player.getStorage();

        if (player.getLevel() < 15) {
            player.message("You may only use this storage once you have reached level 15.");
            return null;
        }
        if (action == 4) { // take out
            if (slot < 0 || slot > storage.getSlots()) { // removal starts at zero
                return null;
            }
            slot = storage.getSlot(MapleInventoryType.getByType(type), (byte) slot);
            Item item = storage.getItem((byte) slot);
            if (item != null) {
                if (MapleItemInformationProvider.getInstance().isPickupRestricted(item.getItemId()) && player.getItemQuantity(item.getItemId(), true) > 0) {
                    getClient().announce(MaplePacketCreator.getStorageError((byte) 0x0C));
                    return null;
                }
                if (player.getMap().getId() == 910000000) {
                    if (player.getMeso() < 1000) {
                        getClient().announce(MaplePacketCreator.getStorageError((byte) 0x0B));
                        return null;
                    } else {
                        player.gainMeso(-1000, false);
                    }
                }
                if (MapleInventoryManipulator.checkSpace(getClient(), item.getItemId(), item.getQuantity(), item.getOwner())) {
                    item = storage.takeOut(slot);//actually the same but idc
                    Server.insertLog(getClass().getSimpleName(), "{} withdrew {} of {}", player.getName(), item.getQuantity(), item.getItemId());
                    if ((item.getFlag() & ItemConstants.KARMA) == ItemConstants.KARMA) {
                        item.setFlag((byte) (item.getFlag() ^ ItemConstants.KARMA)); //items with scissors of karma used on them are reset once traded
                    } else if (item.getType() == 2 && (item.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES) {
                        item.setFlag((byte) (item.getFlag() ^ ItemConstants.SPIKES));
                    }
                    MapleInventoryManipulator.addFromDrop(getClient(), item, false);
                    storage.sendTakenOut(getClient(), ii.getInventoryType(item.getItemId()));
                } else {
                    getClient().announce(MaplePacketCreator.getStorageError((byte) 0x0A));
                }
            }
        } else if (action == 5) { // store
            MapleInventoryType slotType = ii.getInventoryType(itemID);
            MapleInventory Inv = player.getInventory(slotType);
            if (slot < 1 || slot > Inv.getSlotLimit()) { //player inv starts at one
                return null;
            }
            if (quantity < 1 || player.getItemQuantity(itemID, false) < quantity) {
                return null;
            }
            if (storage.isFull()) {
                getClient().announce(MaplePacketCreator.getStorageError((byte) 0x11));
                return null;
            }
            short meso = (short) (player.getMap().getId() == 910000000 ? -500 : -100);
            if (player.getMeso() < meso) {
                getClient().announce(MaplePacketCreator.getStorageError((byte) 0x0B));
            } else {
                MapleInventoryType type = ii.getInventoryType(itemID);
                Item item = player.getInventory(type).getItem(slot).duplicate();
                if (item.getItemId() == itemID && (item.getQuantity() >= quantity || ItemConstants.isRechargable(itemID))) {
                    if (ItemConstants.isRechargable(itemID)) {
                        quantity = item.getQuantity();
                    }
                    player.gainMeso(meso, false, true, false);
                    MapleInventoryManipulator.removeFromSlot(getClient(), type, slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                    storage.sendStored(getClient(), ii.getInventoryType(itemID));
                    Server.insertLog(getClass().getSimpleName(), "{} deposited {} of {}", player.getName(), item.getQuantity(), item.getItemId());
                }
            }
        } else if (action == 7) { // meso
            int storageMesos = storage.getMeso();
            int playerMesos = player.getMeso();
            if ((mesos > 0 && storageMesos >= mesos) || (mesos < 0 && playerMesos >= -mesos)) {
                if (mesos < 0 && (storageMesos - mesos) < 0) {
                    mesos = -2147483648 + storageMesos;
                    if (mesos < playerMesos) {
                        return null;
                    }
                } else if (mesos > 0 && (playerMesos + mesos) < 0) {
                    mesos = 2147483647 - playerMesos;
                    if (mesos > storageMesos) {
                        return null;
                    }
                }
                storage.setMeso(storageMesos - mesos);
                player.gainMeso(mesos, false, true, false);
                Server.insertLog(getClass().getSimpleName(), "{} {} {} mesos", player.getName(), (mesos > 0 ? "deposited" : "withdrew"), Math.abs(mesos));
            } else {
                return null;
            }
            storage.sendMeso(getClient());
        } else if (action == 8) {// close
            storage.close();
        }
        return null;
    }
}