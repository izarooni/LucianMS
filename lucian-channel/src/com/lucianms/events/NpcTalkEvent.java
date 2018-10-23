package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.PlayerNPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

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
        if (!player.isAlive()) {
            setCanceled(true);
            return null;
        }
        MapleMapObject mapObjects = player.getMap().getMapObject(objectId);
        if (mapObjects != null) {
            if (mapObjects instanceof MapleNPC) {
                MapleNPC npc = (MapleNPC) mapObjects;
                if (player.isGM() && player.isDebug()) {
                    player.sendMessage("NPC Talk ObjectID: {}, ID: {}, Script: {}", npc.getObjectId(), npc.getId(), npc.getScript());
                }
//                if (npc.getId() == 9010009) {
//                    player.announce(MaplePacketCreator.sendDuey((byte) 8, DueyHandler.loadItems(player)));
//                } else
                if (npc.hasShop()) {
                    if (player.getShop() == null) {
                        npc.sendShop(getClient());
                    }
                } else {
                    if (getClient().getCM() != null || getClient().getQM() != null) {
                        player.announce(MaplePacketCreator.enableActions());
                        return null;
                    }
                    if (npc.getId() >= 9100100 && npc.getId() <= 9100200) {
                        // Custom handling for gachapon scripts to reduce the amount of scripts needed.
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