package com.lucianms.server.events.channel;

import client.MapleCharacter;
import net.PacketEvent;
import net.server.channel.handlers.DueyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.PlayerNPC;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class NPCTalkEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(NPCTalkEvent.class);
    private int objectId;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        objectId = slea.readInt();
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
                    player.sendMessage("NPC Talk ID: {}, Script: {}", npc.getId(), npc.getScript());
                }
                if (npc.getId() == 9010009) {
                    player.announce(MaplePacketCreator.sendDuey((byte) 8, DueyHandler.loadItems(player)));
                } else if (npc.hasShop()) {
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
                        NPCScriptManager.start(getClient(), npc.getId(), npc.getScript());
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