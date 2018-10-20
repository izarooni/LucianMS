
package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PacketEvent;
import com.lucianms.events.gm.MapleCoconut;
import com.lucianms.events.gm.MapleCoconuts;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerCoconutEvent extends PacketEvent {

    private int ID;

    @Override
    public void processInput(MaplePacketReader reader) {
        ID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        MapleCoconut event = map.getCoconut();
        if (event == null) {
            return null;
        }
        MapleCoconuts nut = event.getCoconut(ID);
        if (!nut.isHittable()) {
            return null;
        }
        if (System.currentTimeMillis() < nut.getHitTime()) {
            return null;
        }
        if (nut.getHits() > 2 && Math.random() < 0.4) {
            if (Math.random() < 0.01 && event.getStopped() > 0) {
                nut.setHittable(false);
                event.stopCoconut();
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, ID, 1));
                return null;
            }
            nut.setHittable(false); // for sure :)
            nut.resetHits(); // For next event (without restarts)
            if (Math.random() < 0.05 && event.getBombings() > 0) {
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, ID, 2));
                event.bombCoconut();
            } else if (event.getFalling() > 0) {
                map.broadcastMessage(MaplePacketCreator.hitCoconut(false, ID, 3));
                event.fallCoconut();
                if (player.getTeam() == 0) {
                    event.addMapleScore();
                    map.broadcastMessage(MaplePacketCreator.serverNotice(5, player.getName() + " of Team Maple knocks down a coconut."));
                } else {
                    event.addStoryScore();
                    map.broadcastMessage(MaplePacketCreator.serverNotice(5, player.getName() + " of Team Story knocks down a coconut."));
                }
                map.broadcastMessage(MaplePacketCreator.coconutScore(event.getMapleScore(), event.getStoryScore()));
            }
        } else {
            nut.hit();
            map.broadcastMessage(MaplePacketCreator.hitCoconut(false, ID, 1));
        }
        return null;
    }
}  
