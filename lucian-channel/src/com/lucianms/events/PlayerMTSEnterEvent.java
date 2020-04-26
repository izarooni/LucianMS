package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerMTSEnterEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!JailManager.isJailed(player.getId())) {
            NPCScriptManager.start(getClient(), 9899972, "f_multipurpose");
        } else {
            player.sendMessage(5, "You cannot do that while jailed.");
        }
        player.announce(MaplePacketCreator.enableActions());
        return null;
    }
}