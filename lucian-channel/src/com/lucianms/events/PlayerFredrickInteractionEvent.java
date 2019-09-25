package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.ItemFactory;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerFredrickInteractionEvent extends PacketEvent {

    private byte action;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        switch (action) {
            case 25:
                // c.announce(MaplePacketCreator.getFredrick((byte) 0x24));
                break;
            case 26:
                List<Pair<Item, MapleInventoryType>> items;
                try (Connection con = getClient().getWorldServer().getConnection()) {
                    items = ItemFactory.MERCHANT.loadItems(con, player.getId(), false);
                    if (!check(player, items)) {
                        getClient().announce(MaplePacketCreator.fredrickMessage((byte) 0x21));
                        return null;
                    }
                    player.gainMeso(player.getMerchantMeso(), false);
                    player.setMerchantMeso(0);
                    if (deleteItems(player)) {
                        if (player.getHiredMerchant() != null)
                            player.getHiredMerchant().clearItems();

                        for (Pair<Item, MapleInventoryType> p : items) {
                            Item left = p.getLeft();
                            MapleInventoryManipulator.addFromDrop(getClient(), left, false);
                            Server.insertLog(getClass().getSimpleName(), "{} gained {} of {}", player.getName(), left.getQuantity(), left.getItemId());
                        }
                        getClient().announce(MaplePacketCreator.fredrickMessage((byte) 0x1E));

                    } else {
                        player.message("An unknown error has occurred.");
                    }
                    break;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                break;
            case 0x1C: //Exit
                break;
        }
        return null;
    }

    private static boolean check(MapleCharacter chr, List<Pair<Item, MapleInventoryType>> items) {
        if (chr.getMeso() + chr.getMerchantMeso() < 0) {
            return false;
        }
        return MapleInventory.checkSpots(chr, items);
    }

    private static boolean deleteItems(MapleCharacter chr) {
        try (Connection con = chr.getClient().getWorldServer().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?")) {
            ps.setInt(1, ItemFactory.MERCHANT.getValue());
            ps.setInt(2, chr.getId());
            ps.execute();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
