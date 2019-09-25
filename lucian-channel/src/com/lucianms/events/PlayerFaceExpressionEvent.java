package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerFaceExpressionEvent extends PacketEvent {

    private int emote;

    @Override
    public void processInput(MaplePacketReader reader) {
        emote = reader.readInt();
        if (emote > 7) {
            int itemID = 5159992 + emote;
            if (getClient().getPlayer().getInventory(ItemConstants.getInventoryType(itemID)).findById(itemID) == null) {
                setCanceled(true);
            }
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        player.getMap().broadcastMessage(player, MaplePacketCreator.facialExpression(player, emote), false);
        return null;
    }
}
