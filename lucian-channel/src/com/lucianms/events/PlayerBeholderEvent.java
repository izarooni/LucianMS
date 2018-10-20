package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.skills.DarkKnight;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleSummon;

/**
 * @author BubblesDev
 * @author izarooni
 */
public class PlayerBeholderEvent extends PacketEvent {

    private int objectID;
    private int skillID;
    private MapleSummon summon;

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        objectID = reader.readInt();
        summon = player.getSummons().values().stream().filter(s -> s.getObjectId() == objectID).findFirst().orElse(null);
        if (summon != null) {
            skillID = reader.readInt();
            if (skillID == DarkKnight.AURA_OF_THE_BEHOLDER) {
//                reader.readShort(); //Not sure.
            } else if (skillID == DarkKnight.HEX_OF_THE_BEHOLDER) {
//                reader.readByte(); //Not sure.
            }
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (summon == null) {
            player.getSummons().clear();
        }
        return null;
    }
}
