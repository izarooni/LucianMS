package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class AdministratorCommandEvent extends PacketEvent {

    private byte action;
    private boolean bool;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            default:
                getLogger().info(reader.toString());
                break;
            case 0x11:
                break;
            case 0x10:
                bool = reader.readByte() != 0;
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isGM()) {
            return null;
        }
        switch (action) {
            case 0x11: {
                StringBuilder sb = new StringBuilder();
                ArrayList<MapleCharacter> characters = new ArrayList<>(player.getMap().getCharacters());
                for (MapleCharacter character : characters) {
                    sb.append(character.getName()).append(", ");
                }
                if (!characters.isEmpty()) {
                    sb.setLength(sb.length() - 2);
                }
                player.sendMessage("Players currently in the map:");
                player.sendMessage(sb.toString());
                sb.setLength(0);
                break;
            }
            case 0x10:
                player.setHidden(bool, false);
                break;
        }
        return null;
    }
}
