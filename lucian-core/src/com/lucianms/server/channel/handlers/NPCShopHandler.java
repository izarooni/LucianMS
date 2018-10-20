package com.lucianms.server.channel.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.constants.ItemConstants;

/**
 * @author izarooni
 */
public class NPCShopHandler extends PacketEvent {

    private byte action;
    private short slot;
    private short quantity;
    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        action = reader.readByte();
        switch (action) {
            case 0: // buy
                slot = reader.readShort();
                itemID = reader.readInt();
                quantity = reader.readShort();
                if (quantity < 1 || player.getShop() == null) {
                    setCanceled(true);
                }
                break;
            case 1: // sell
                slot = reader.readShort();
                itemID = reader.readInt();
                quantity = reader.readShort();
                if (quantity < 1 || player.getShop() == null) {
                    setCanceled(true);
                }
                break;
            case 2: // recharge
                slot = reader.readShort();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        switch (action) {
            case 0:
                player.getShop().buy(getClient(), slot, itemID, quantity);
                break;
            case 1:
                player.getShop().sell(getClient(), ItemConstants.getInventoryType(itemID), slot, quantity);
                break;
            case 2:
                player.getShop().recharge(getClient(), slot);
                break;
            case 3:
                player.setShop(null);
                break;
        }
        return null;
    }
}
