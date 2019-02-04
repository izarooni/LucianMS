package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PacketEvent;
import com.lucianms.events.gm.MapleSnowball;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerSnowballEvent extends PacketEvent {

    private byte action;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter chr = getClient().getPlayer();
        MapleMap map = chr.getMap();
        final MapleSnowball snowball = map.getSnowball(chr.getTeam());
        if (snowball == null) {
            return null;
        }
        final MapleSnowball otherSnowball = map.getSnowball(chr.getTeam() == 0 ? (byte) 1 : 0);
        if (otherSnowball == null || snowball.getSnowmanHP() == 0) {
            return null;
        }
        if ((System.currentTimeMillis() - chr.getLastSnowballAttack()) < 500) {
            return null;
        }
        if (chr.getTeam() != (action % 2)) {
            return null;
        }

        chr.setLastSnowballAttack(System.currentTimeMillis());
        int damage = 0;
        if (action < 2 && otherSnowball.getSnowmanHP() > 0) {
            damage = 10;
        } else if (action == 2 || action == 3) {
            if (Math.random() < 0.03) {
                damage = 45;
            } else {
                damage = 15;
            }
        }

        if (action >= 0 && action <= 4) {
            snowball.hit(action, damage);
        }
        return null;
    }
}
