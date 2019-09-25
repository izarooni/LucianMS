package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.io.scripting.npc.NPCConversationManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.PlayerNPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class NpcTalkEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(NpcTalkEvent.class);
    private int objectId;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectId = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spam = player.getSpamTracker(SpamTracker.SpamOperation.NpcTalk);
        if (!player.isAlive() || spam.testFor(150)) {
            return null;
        }
        NPCConversationManager cm = getClient().getCM();
        if (cm != null && cm.isProc()) {
            return null;
        }

        MapleMapObject mapObjects = player.getMap().getMapObject(objectId);
        if (mapObjects != null) {
            if (mapObjects instanceof MapleNPC) {
                MapleNPC npc = (MapleNPC) mapObjects;
                if (player.isGM() && player.isDebug()) {
                    player.sendMessage("NPC Talk ObjectID: {}, ID: {}, Script: {}", npc.getObjectId(), npc.getId(), npc.getScript());
                }
                if (npc.hasShop()) {
                    if (player.getShop() == null) {
                        npc.sendShop(getClient());
                    }
                } else {
                    if (npc.getId() >= 9100100 && npc.getId() <= 9100200) {
                        NPCScriptManager.start(getClient(), npc.getId(), "gachapon");
                    } else {
                        NPCScriptManager.start(getClient(), npc.getObjectId(), npc.getId(), npc.getScript());
                    }
                }
            } else if (mapObjects instanceof PlayerNPC) {
                PlayerNPC npc = (PlayerNPC) mapObjects;
                if (player.isGM() && player.isDebug()) {
                    player.sendMessage("NPC Talk ID: {}, Script: {}", npc.getId(), npc.getScript());
                }
                NPCScriptManager.start(getClient(), npc.getId(), npc.getScript());
            } else {
                LOGGER.warn("{} attempted to speak to non-npc map object {}", player.getName(), mapObjects.getType().name());
            }
        }
        return null;
    }
}