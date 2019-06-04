package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleRing;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.CashShop;
import com.lucianms.server.CashShop.CashItem;
import com.lucianms.server.CashShop.CashItemFactory;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @author izarooni
 */
public class PlayerCashShopOperationEvent extends PacketEvent {

    public static byte[] getCashItemMoveFailed(byte result) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        w.write(105);
        w.write(result);
        return w.getPacket();
    }

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
            case 0x0d:
            case 0x20: {
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
        MapleClient client = getClient();
        MapleCharacter player = client.getPlayer();
        MapleWorld world = client.getWorldServer();
        MapleChannel ch = client.getChannelServer();
        CashShop cs = player.getCashShop();

        if (!cs.isOpened()) {
            client.announce(MaplePacketCreator.enableActions());
            return null;
        }
        switch (action) {
            case 0x03:
            case 0x1E: {
                CashItem cItem = CashItemFactory.getItem(SN);
                if (cItem == null || !cItem.isOnSale() || cs.getCash(cost) < cItem.getPrice()) {
                    return null;
                }
                int itemID = cItem.getItemId();
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + cItem.getImgdir();
                    content += "\r\nItem ID: " + itemID;
                    content += "\r\nSN: " + SN;
                    client.announce(MaplePacketCreator.serverNotice(1, content));
                    client.announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                if (action == 0x03) { // Item
                    Item item = cItem.toItem();
                    cs.addToInventory(item);
                    client.announce(MaplePacketCreator.showBoughtCashItem(item, client.getAccID()));
                } else { // Package
                    List<Item> cashPackage = CashItemFactory.getPackage(cItem.getItemId());
                    for (Item item : cashPackage) {
                        if (item == null) {
                            player.sendMessage(1, "We failed to create an item in this package.\r\nCode: {}", SN);
                            client.announce(MaplePacketCreator.showCash(player));
                            return null;
                        }
                        cs.addToInventory(item);
                    }
                    client.announce(MaplePacketCreator.showBoughtCashPackage(cashPackage, client.getAccID()));
                }
                cs.gainCash(cost, -cItem.getPrice());
                client.announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x04: {
                CashItem cItem = CashItemFactory.getItem(itemID);
                Map<String, String> recipient = MapleCharacter.getCharacterFromDatabase(username);
                if (prohibitPurchase(cItem, cs.getCash(4)) || content.length() < 1 || content.length() > 73) {
                    client.announce(getCashItemMoveFailed((byte) 165));
                    return null;
                }
                if (!checkBirthday(client, birthday)) {
                    client.announce(MaplePacketCreator.showCashShopMessage((byte) 0xC4));
                    return null;
                } else if (recipient == null) {
                    client.announce(MaplePacketCreator.showCashShopMessage((byte) 0xA9));
                    return null;
                } else if (recipient.get("accountid").equals(String.valueOf(client.getAccID()))) {
                    client.announce(MaplePacketCreator.showCashShopMessage((byte) 0xA8));
                    return null;
                }
                cs.gift(Integer.parseInt(recipient.get("id")), player.getName(), content, cItem.getSN());
                client.announce(MaplePacketCreator.showGiftSucceed(recipient.get("name"), cItem));
                cs.gainCash(4, -cItem.getPrice());
                client.announce(MaplePacketCreator.showCash(player));
                player.sendNote(recipient.get("name"), player.getName() + " has sent you a gift! Go check out the Cash Shop.", (byte) 0); //fame or not
                MapleCharacter receiver = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(recipient.get("name")));
                if (receiver != null) receiver.showNote();
                break;
            }
            case 0x05: {
                client.announce(MaplePacketCreator.showWishList(player, true));
                break;
            }
            case 0x06: {
                if (subAction == 0) {
                    if (cs.getCash(cashType) < 4000) {
                        return null;
                    }
                    if (player.gainSlots(inventoryType, 4, false)) {
                        client.announce(MaplePacketCreator.showBoughtInventorySlots(inventoryType, player.getSlots(inventoryType)));
                        cs.gainCash(cashType, -4000);
                        client.announce(MaplePacketCreator.showCash(player));
                    }
                } else {
                    CashItem cItem = CashItemFactory.getItem(itemID);
                    int type = (cItem.getItemId() - 9110000) / 1000;
                    if (prohibitPurchase(cItem, cs.getCash(type))) {
                        client.announce(getCashItemMoveFailed((byte) 165));
                        return null;
                    }
                    if (player.gainSlots(type, 8, false)) {
                        client.announce(MaplePacketCreator.showBoughtInventorySlots(type, player.getSlots(type)));
                        cs.gainCash(type, -cItem.getPrice());
                        client.announce(MaplePacketCreator.showCash(player));
                    }
                }
                break;
            }
            case 0x07: {
                if (subAction == 0) {
                    if (cs.getCash(cashType) < 4000) {
                        return null;
                    }
                    if (player.getStorage().increaseSlotCount(4)) {
                        client.announce(MaplePacketCreator.showBoughtStorageSlots(player.getStorage().getSlotCount()));
                        cs.gainCash(cashType, -4000);
                        client.announce(MaplePacketCreator.showCash(player));
                    }
                } else {
                    CashItem cItem = CashItemFactory.getItem(itemID);
                    if (prohibitPurchase(cItem, cs.getCash(cashType))) {
                        client.announce(getCashItemMoveFailed((byte) 165));
                        return null;
                    }
                    if (player.getStorage().increaseSlotCount(8)) {
                        client.announce(MaplePacketCreator.showBoughtStorageSlots(player.getStorage().getSlotCount()));
                        cs.gainCash(cashType, -cItem.getPrice());
                        client.announce(MaplePacketCreator.showCash(player));
                    }
                }
                break;
            }
            case 0x08: {
                CashItem cItem = CashItemFactory.getItem(itemID);
                if (prohibitPurchase(cItem, cs.getCash(cashType))) {
                    client.announce(getCashItemMoveFailed((byte) 165));
                    return null;
                } else if (client.gainCharacterSlot()) {
                    client.announce(MaplePacketCreator.showBoughtCharacterSlot(client.getCharacterSlots()));
                    cs.gainCash(cashType, -cItem.getPrice());
                    client.announce(MaplePacketCreator.showCash(player));
                }
                break;
            }
            case 0x0D: {
                Item item = cs.findByCashId(itemID);
                if (item == null) {
                    return null;
                }
                MapleInventory inventory = player.getInventory(ItemConstants.getInventoryType(item.getItemId()));
                if (inventory.getNextFreeSlot() == -1) {
                    return null;
                }
                if (item instanceof Equip) {
                    Equip equip = (Equip) item;
                    if (equip.getRingId() >= 0) {
                        try {
                            MapleRing ring = MapleRing.load(equip.getRingId());
                            if (ItemConstants.isFriendshipEquip(item.getItemId())) {
                                player.getFriendshipRings().add(ring);
                            } else if (ItemConstants.isCoupleEquip(item.getItemId())) {
                                player.getCrushRings().add(ring);
                            }
                        } catch (SQLException e) {
                            client.announce(getCashItemMoveFailed((byte) 187));
                            return null;
                        }
                    }
                }
                if (inventory.addItem(item) != -1) {
                    cs.removeFromInventory(item);
                    client.announce(MaplePacketCreator.takeFromCashInventory(item));
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
                client.announce(MaplePacketCreator.putIntoCashInventory(item, client.getAccID()));
                break;
            }
            case 0x1d: {
                CashItem csItem = CashItemFactory.getItem(SN);
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + csItem.getImgdir();
                    content += "\r\nItem ID: " + csItem.getItemId();
                    content += "\r\nSN: " + SN;
                    client.announce(MaplePacketCreator.serverNotice(1, content));
                    client.announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                MapleCharacter partner = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (partner == null || partner.getClient().getChannel() != client.getChannel()) {
                    player.getClient().announce(MaplePacketCreator.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
                } else {
                    if (csItem.toItem() instanceof Equip) {
                        Equip item = (Equip) csItem.toItem();
                        int ringID;
                        try {
                            ringID = MapleRing.create(csItem.getItemId(), player, partner);
                            item.setRingId(ringID);
                        } catch (SQLException e) {
                            getLogger().error("Failed to create ring between {} and {}", player.getName(), partner.getName(), e);
                            client.announce(getCashItemMoveFailed((byte) 0));
                            return null;
                        }
                        cs.gainCash(cashType, -csItem.getPrice());
                        cs.addToInventory(item);
                        client.announce(MaplePacketCreator.showBoughtCashItem(item, client.getAccID()));
                        cs.gift(partner.getId(), player.getName(), content, item.getSN(), (ringID + 1));

                        player.sendNote(partner.getName(), content, (byte) 1);
                        partner.showNote();
                    }
                }
                client.announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x20: {
                int itemId = CashItemFactory.getItem(itemID).getItemId();
                if (player.getMeso() > 0) {
                    if (itemId == 4031180 || itemId == 4031192 || itemId == 4031191) {
                        player.gainMeso(-1, false);
                        MapleInventoryManipulator.addById(client, itemId, (short) 1);
                        client.announce(MaplePacketCreator.showBoughtQuestItem(itemId));
                    }
                }
                client.announce(MaplePacketCreator.showCash(player));
                break;
            }
            case 0x23: {
                CashItem csItem = CashItemFactory.getItem(SN);
                if (player.isDebug()) {
                    String content = "[Debug]";
                    content += "\r\nImgdir: " + csItem.getImgdir();
                    content += "\r\nItem ID: " + csItem.getItemId();
                    content += "\r\nSN: " + SN;
                    client.announce(MaplePacketCreator.serverNotice(1, content));
                    client.announce(MaplePacketCreator.showCash(player));
                    return null;
                }
                MapleCharacter partner = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (partner == null || partner.getClient().getChannel() != ch.getId()) {
                    player.dropMessage("The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel.");
                } else {
                    // Need to check to make sure its actually an equip and the right SN...
                    if (csItem.toItem() instanceof Equip) {
                        Equip item = (Equip) csItem.toItem();
                        int ringID;
                        try {
                            ringID = MapleRing.create(csItem.getItemId(), player, partner);
                            item.setRingId(ringID);
                        } catch (SQLException e) {
                            getLogger().error("Failed to create ring between {} and {}", player.getName(), partner.getName(), e);
                            client.announce(getCashItemMoveFailed((byte) 0));
                            return null;
                        }
                        cs.gainCash(cashType, -csItem.getPrice());
                        cs.addToInventory(item);
                        client.announce(MaplePacketCreator.showBoughtCashItem(item, client.getAccID()));
                        cs.gift(partner.getId(), player.getName(), content, item.getSN(), (ringID + 1));
                        player.sendNote(partner.getName(), content, (byte) 1);
                        partner.showNote();
                    }
                }
                client.announce(MaplePacketCreator.showCash(player));
                break;
            }
        }
        return null;
    }

    private static boolean checkBirthday(MapleClient c, int nDate) {
        int year = nDate / 10000;
        int month = (nDate - year * 10000) / 100;
        int day = nDate - year * 10000 - month * 100;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day);
        return c.checkBirthDate(cal);
    }

    private static boolean prohibitPurchase(CashItem item, int cash) {
        return item == null || !item.isOnSale() || item.getPrice() > cash || isBlocked(item.getItemId());
    }

    private static boolean isBlocked(int id) {
        int type = id / 10000;
        return type == 521; // rate modifier coupons
    }
}
