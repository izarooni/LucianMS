
package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.features.coconut.CoconutEvent;
import com.lucianms.features.coconut.CoconutObject;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class CoconutHitEvent extends PacketEvent {

    private short coconutID;
    private short unk;

    @Override
    public void processInput(MaplePacketReader reader) {
        coconutID = reader.readShort();
        unk = reader.readShort();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        CoconutEvent event = map.getCoconut();
        if (event == null) {
            return null;
        }
        CoconutObject nut = event.getCoconuts().get(coconutID);
        if (nut == null) {
            return null;
        } else if (!nut.isCanHit()) {
            if (nut.getResult() == 1) {
                // 'stop' type coconuts (to distract players)
                map.sendPacket(MaplePacketCreator.getCoconutHit(coconutID, unk, (byte) 1));
            }
            return null;
        }
        byte nState = nut.getState();
        nut.setState(++nState);
        if (player.isDebug()) {
            player.sendMessage("Updating coconut '{}' state to {}", nut.getName(), nState);
        }
        if (nState == event.getHit()) {
            nut.setCanHit(false);
            map.sendPacket(MaplePacketCreator.getCoconutHit(coconutID, unk, nut.getResult()));
            if (nut.getResult() == 3) { // 'falling' type coconut
                if (player.getTeam() == 0) {
                    event.setRedPoints(event.getRedPoints() + 1);
                } else {
                    event.setBluePoints(event.getBluePoints() + 1);
                }
                map.sendPacket(MaplePacketCreator.getCoconutScore(event.getRedPoints(), event.getBluePoints()));
            }
        } else {
            map.sendPacket(MaplePacketCreator.getCoconutHit(coconutID, unk, (byte) 1));
        }
        return null;
    }
}  
