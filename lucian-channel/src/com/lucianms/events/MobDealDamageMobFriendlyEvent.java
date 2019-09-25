package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.Randomizer;

/**
 * @author Xotic & BubblesDev
 * @author izarooni
 */

public class MobDealDamageMobFriendlyEvent extends PacketEvent {

    private int attackerID;
    private int damagedID;

    @Override
    public void processInput(MaplePacketReader reader) {
        attackerID = reader.readInt();
        reader.skip(4);
        damagedID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMonster dMonster = player.getMap().getMonsterByOid(damagedID);
        MapleMonster aMonster = player.getMap().getMonsterByOid(attackerID);
        if (dMonster == null || aMonster == null) {
            return null;
        }

        int localMaxHP = (int) Math.min(Integer.MAX_VALUE, dMonster.getMaxHp());

        int damage = Randomizer.nextInt((((localMaxHP / 13) + (dMonster.getPADamage() * 10)) * 2) + 500) / 10; // Beng's forumla.
        if (dMonster.getId() == 9300061) { // Moon Bunny
            if (dMonster.getHp() - damage < 1) {
                dMonster.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny went home because he was sick."));
                dMonster.getMap().killFriendlies(dMonster);
                EventInstanceManager eim = dMonster.getEventInstance();
                eim.disbandParty();
            }
            dMonster.getMap().addBunnyHit();
        }

        dMonster.setHp(dMonster.getHp() - damage);
        if (dMonster.getHp() < 0) {
            dMonster.setHp(0);
        }
        int localHP = (int) Math.min(Integer.MAX_VALUE, dMonster.getHp());
        dMonster.getMap().broadcastMessage(MaplePacketCreator.MobDamageMobFriendly(dMonster, damage, localHP, localMaxHP), dMonster.getPosition());
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}