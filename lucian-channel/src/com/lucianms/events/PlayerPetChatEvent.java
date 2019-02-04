package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerPetChatEvent extends PacketEvent {

    private String content;
    private byte action;
    private int petID;

    @Override
    public void processInput(MaplePacketReader reader) {
        petID = reader.readInt();
        reader.readInt();
        reader.readByte();
        action = reader.readByte();
        String content = reader.readMapleAsciiString();
        if (petID < 0 || petID > 3 || action < 0 || action > 9) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        byte pet = player.getPetIndex(petID);
        if (pet == -1 || content.length() > Byte.MAX_VALUE) {
            return null;
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.petChat(player.getId(), pet, action, content), true);
        return null;
    }
}
