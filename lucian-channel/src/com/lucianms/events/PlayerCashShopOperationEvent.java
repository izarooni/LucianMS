package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleRing;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.constants.ItemConstants;
import com.lucianms.server.CashShop;
import com.lucianms.server.CashShop.CashItem;
import com.lucianms.server.CashShop.CashItemFactory;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import tools.MaplePacketCreator;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @author izarooni
 */
public class PlayerCashShopOperationEvent extends PacketEvent {

    private String username;
    private String content;
    private int cost;
    private int SN;
    private int birthday;
    private int itemID;
    private int cashType;
    private short available;
    private byte action;
    private byte subAction;
    private byte type;
    private byte inventoryType;
    private int[] wishlist;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 0x03:
            case 0x1e: {
                reader.readByte();
                cost = reader.readInt();
                SN = reader.readInt();
                break;
            }
            case 0x04: {
                birthday = reader.readInt();
                itemID = reader.readInt();
                username = reader.readMapleAsciiString();
                content = reader.readMapleAsciiString();
                break;
            }
            case 0x05: {
                CashShop cashShop = getClient().getPlayer().getCashShop();
                cashShop.clearWishList();
                wishlist = new int[10];
                for (int i = 0; i < 10; i++) {
                    int SN = reader.readInt();
                    CashItem item = CashItemFactory.getItem(SN);
                    if (item != null && item.isOnSale() && SN != 0) {
                        cashShop.addToWishList(SN);
                    }
                }
                break;
            }
            case 0x06: {
                reader.skip(1);
                cashType = reader.readInt();
                subAction = reader.readByte();
                if (subAction == 0) {
                    inventoryType = reader.readByte();
                } else {
                    itemID = reader.readInt();
                }
                break;
            }
            case 0x07: {
                reader.skip(1);
                cashType = reader.readInt();
                subAction = reader.readByte();
                if (subAction != 0) {
                    itemID = reader.readInt();
                }
                break;
            }
            case 0x08: {
                reader.skip(1);
                cashType = reader.readInt();
                itemID = reader.readInt();
                break;
            }
            case 0x0d: {
                itemID = reader.readInt();
                break;
            }
            case 0x0e: {
                itemID = reader.readInt();
                reader.skip(4);
                inventoryType = reader.readByte();
                break;
            }
            case 0x1d: {
                birthday = reader.readInt();
                cashType = reader.readInt();
                SN = reader.readInt();
                username = reader.readMapleAsciiString();
                content = reader.readMapleAsciiString();
                break;
            }
            case 0x20: {
                itemID = reader.readInt();
                break;
            }
            case 0x23: {
                birthday = reader.readInt();
                cashType = reader.readInt();
                SN = reader.readInt();
                username = reader.readMapleAsciiString();
                available = (short) (reader.readShort() - 1);
                content = reader.readAsciiString(available);
                reader.skip(1);
                break;
            }
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        CashShop cs = player.getCashShop();
        if (!cs.isOpened()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        switch (action) {
            case 0x03:
            case 0x1e: {
                CashItem cItem = CashItemFactory.getItem(SN);
                if (cItem == null || !cItem.isOnSale() || cs.getCash(cost) < cItem.getPrice()) {
                    return null;
                }
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + cItem.getImgdir();
                    content += "\r\nItem ID: " + cItem.getItemId();
                    content += "\r\nSN: " + SN;
                    getClient().announce(MaplePacketCreator.serverNotice(1, content));
                    getClient().announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                if (ItemConstants.isPet(cItem.getItemId())) {
                    int itemID = cItem.getItemId();
                    if (itemID == 5000014 || itemID == 5000022) {
                        getClient().announce(MaplePacketCreator.serverNotice(1, "These pets are available for our donors only!"));
                        getClient().announce(MaplePacketCreator.showCash(player));
                        return null;
                    }
                }
                if (action == 0x03) { // Item
                    Item item = cItem.toItem();
                    cs.addToInventory(item);
                    getClient().announce(MaplePacketCreator.showBoughtCashItem(item, getClient().getAccID()));
                } else { // Package
                    List<Item> cashPackage = CashItemFactory.getPackage(cItem.getItemId());
                    for (Item item : cashPackage) {
                        if (item == null) {
                            player.sendMessage(1, "We failed to create an item in this package.\r\nCode: 0x{}", Integer.toHexString(SN));
                            getClient().announce(MaplePacketCreator.showCash(player));
                            return null;
                        }
                        cs.addToInventory(item);
                    }
                    getClient().announce(MaplePacketCreator.showBoughtCashPackage(cashPackage, getClient().getAccID()));
                }
                cs.gainCash(cost, -cItem.getPrice());
                getClient().announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x04: {
                CashItem cItem = CashItemFactory.getItem(itemID);
                Map<String, String> recipient = MapleCharacter.getCharacterFromDatabase(username);
                if (!canBuy(cItem, cs.getCash(4)) || content.length() < 1 || content.length() > 73) {
                    return null;
                }
                if (!checkBirthday(getClient(), birthday)) {
                    getClient().announce(MaplePacketCreator.showCashShopMessage((byte) 0xC4));
                    return null;
                } else if (recipient == null) {
                    getClient().announce(MaplePacketCreator.showCashShopMessage((byte) 0xA9));
                    return null;
                } else if (recipient.get("accountid").equals(String.valueOf(getClient().getAccID()))) {
                    getClient().announce(MaplePacketCreator.showCashShopMessage((byte) 0xA8));
                    return null;
                }
                cs.gift(Integer.parseInt(recipient.get("id")), player.getName(), content, cItem.getSN());
                getClient().announce(MaplePacketCreator.showGiftSucceed(recipient.get("name"), cItem));
                cs.gainCash(4, -cItem.getPrice());
                getClient().announce(MaplePacketCreator.showCash(player));
                player.sendNote(recipient.get("name"), player.getName() + " has sent you a gift! Go check out the Cash Shop.", (byte) 0); //fame or not
                MapleCharacter receiver = getClient().getChannelServer().getPlayerStorage().getPlayerByName(recipient.get("name"));
                if (receiver != null) receiver.showNote();
                break;
            }
            case 0x05: {
                getClient().announce(MaplePacketCreator.showWishList(player, true));
                break;
            }
            case 0x06: {
                if (subAction == 0) {
                    if (cs.getCash(cashType) < 4000) {
                        return null;
                    }
                    if (player.gainSlots(inventoryType, 4, false)) {
                        getClient().announce(MaplePacketCreator.showBoughtInventorySlots(inventoryType, player.getSlots(inventoryType)));
                        cs.gainCash(cashType, -4000);
                        getClient().announce(MaplePacketCreator.showCash(player));
                    }
                } else {
                    CashItem cItem = CashItemFactory.getItem(itemID);
                    int type = (cItem.getItemId() - 9110000) / 1000;
                    if (!canBuy(cItem, cs.getCash(type))) {
                        return null;
                    }
                    if (player.gainSlots(type, 8, false)) {
                        getClient().announce(MaplePacketCreator.showBoughtInventorySlots(type, player.getSlots(type)));
                        cs.gainCash(type, -cItem.getPrice());
                        getClient().announce(MaplePacketCreator.showCash(player));
                    }
                }
                break;
            }
            case 0x07: {
                if (subAction == 0) {
                    if (cs.getCash(cashType) < 4000) {
                        return null;
                    }
                    if (player.getStorage().gainSlots(4)) {
                        getClient().announce(MaplePacketCreator.showBoughtStorageSlots(player.getStorage().getSlots()));
                        cs.gainCash(cashType, -4000);
                        getClient().announce(MaplePacketCreator.showCash(player));
                    }
                } else {
                    CashItem cItem = CashItemFactory.getItem(itemID);

                    if (!canBuy(cItem, cs.getCash(cashType))) {
                        return null;
                    }
                    if (player.getStorage().gainSlots(8)) {
                        getClient().announce(MaplePacketCreator.showBoughtStorageSlots(player.getStorage().getSlots()));
                        cs.gainCash(cashType, -cItem.getPrice());
                        getClient().announce(MaplePacketCreator.showCash(player));
                    }
                }
                break;
            }
            case 0x08: {
                CashItem cItem = CashItemFactory.getItem(itemID);

                if (!canBuy(cItem, cs.getCash(cashType))) {
                    return null;
                }

                if (getClient().gainCharacterSlot()) {
                    getClient().announce(MaplePacketCreator.showBoughtCharacterSlot(getClient().getCharacterSlots()));
                    cs.gainCash(cashType, -cItem.getPrice());
                    getClient().announce(MaplePacketCreator.showCash(player));
                }
                break;
            }
            case 0x0d: {
                Item item = cs.findByCashId(itemID);
                if (item == null) {
                    return null;
                }
                if (player.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(item.getItemId())).addItem(item) != -1) {
                    cs.removeFromInventory(item);
                    getClient().announce(MaplePacketCreator.takeFromCashInventory(item));
                    if (item instanceof Equip) {
                        Equip equip = (Equip) item;
                        if (equip.getRingId() >= 0) {
                            MapleRing ring = MapleRing.loadFromDb(equip.getRingId());
                            if (ring.getItemId() > 1112012) {
                                player.addFriendshipRing(ring);
                            } else {
                                player.addCrushRing(ring);
                            }
                        }
                    }
                }
                break;
            }
            case 0x0e: {
                MapleInventory mi = player.getInventory(MapleInventoryType.getByType(inventoryType));
                Item item = mi.findByCashId(itemID);
                if (item == null) {
                    return null;
                }
                cs.addToInventory(item);
                mi.removeSlot(item.getPosition());
                getClient().announce(MaplePacketCreator.putIntoCashInventory(item, getClient().getAccID()));
                break;
            }
            case 0x1d: {
                CashItem ring = CashItemFactory.getItem(SN);
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + ring.getImgdir();
                    content += "\r\nItem ID: " + ring.getItemId();
                    content += "\r\nSN: " + SN;
                    getClient().announce(MaplePacketCreator.serverNotice(1, content));
                    getClient().announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                MapleCharacter partner = getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
                if (partner == null) {
                    player.getClient().announce(MaplePacketCreator.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
                } else {
                    if (ring.toItem() instanceof Equip) {
                        Equip item = (Equip) ring.toItem();
                        int ringid = MapleRing.createRing(ring.getItemId(), player, partner);
                        item.setRingId(ringid);
                        cs.addToInventory(item);
                        getClient().announce(MaplePacketCreator.showBoughtCashItem(item, getClient().getAccID()));
                        cs.gift(partner.getId(), player.getName(), content, item.getSN(), (ringid + 1));
                        cs.gainCash(cashType, -ring.getPrice());
                        player.addCrushRing(MapleRing.loadFromDb(ringid));
                        player.sendNote(partner.getName(), content, (byte) 1);
                        partner.showNote();
                    }
                }
                getClient().announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x20: {
                int itemId = CashItemFactory.getItem(itemID).getItemId();
                if (player.getMeso() > 0) {
                    if (itemId == 4031180 || itemId == 4031192 || itemId == 4031191) {
                        player.gainMeso(-1, false);
                        MapleInventoryManipulator.addById(getClient(), itemId, (short) 1);
                        getClient().announce(MaplePacketCreator.showBoughtQuestItem(itemId));
                    }
                }
                getClient().announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x23: {
                CashItem ring = CashItemFactory.getItem(SN);
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + ring.getImgdir();
                    content += "\r\nItem ID: " + ring.getItemId();
                    content += "\r\nSN: " + SN;
                    getClient().announce(MaplePacketCreator.serverNotice(1, content));
                    getClient().announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                MapleCharacter partner = getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
                if (partner == null) {
                    player.dropMessage("The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel.");
                } else {
                    // Need to check to make sure its actually an equip and the right SN...
                    if (ring.toItem() instanceof Equip) {
                        Equip item = (Equip) ring.toItem();
                        int ringid = MapleRing.createRing(ring.getItemId(), player, partner);
                        item.setRingId(ringid);
                        cs.addToInventory(item);
                        getClient().announce(MaplePacketCreator.showBoughtCashItem(item, getClient().getAccID()));
                        cs.gift(partner.getId(), player.getName(), content, item.getSN(), (ringid + 1));
                        cs.gainCash(cashType, -ring.getPrice());
                        player.addFriendshipRing(MapleRing.loadFromDb(ringid));
                        player.sendNote(partner.getName(), content, (byte) 1);
                        partner.showNote();
                    }
                }
                getClient().announce(MaplePacketCreator.showCash(player));
                break;
            }
        }
        return null;
    }

    private static boolean checkBirthday(MapleClient c, int idate) {
        int year = idate / 10000;
        int month = (idate - year * 10000) / 100;
        int day = idate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }

    public static boolean canBuy(CashItem item, int cash) {
        return item != null && item.isOnSale() && item.getPrice() <= cash && !blocked(item.getItemId());
    }

    public static boolean blocked(int id) {
        switch (id) { //All 2x exp cards
            case 5211000:
            case 5211004:
            case 5211005:
            case 5211006:
            case 5211007:
            case 5211008:
            case 5211009:
            case 5211010:
            case 5211011:
            case 5211012:
            case 5211013:
            case 5211014:
            case 5211015:
            case 5211016:
            case 5211017:
            case 5211018:
            case 5211037:
            case 5211038:
            case 5211039:
            case 5211040:
            case 5211041:
            case 5211042:
            case 5211043:
            case 5211044:
            case 5211045:
            case 5211049:
                return true;
            default:
                return false;
        }
    }
}
