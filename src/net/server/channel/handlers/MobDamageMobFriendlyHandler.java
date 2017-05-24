package net.server.channel.handlers;

import client.MapleCharacter;
import net.PacketHandler;
import scripting.event.EventInstanceManager;
import server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Xotic & BubblesDev
 * @author izarooni
 */

public class MobDamageMobFriendlyHandler extends PacketHandler {

    private int attackerID;
    private int damagedID;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        attackerID = slea.readInt();
        slea.skip(4);
        damagedID = slea.readInt();
    }

    @Override
    public void onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMonster dMonster = player.getMap().getMonsterByOid(damagedID);
        MapleMonster aMonster = player.getMap().getMonsterByOid(attackerID);
        if (dMonster == null || aMonster == null) {
            return;
        }
        int damage = Randomizer.nextInt(((dMonster.getMaxHp() / 13 + dMonster.getPADamage() * 10)) * 2 + 500) / 10; // Beng's forumla.
        if (aMonster.getId() == 9300061) { // Moon Bunny
            if (dMonster.getHp() - damage < 1) {
                dMonster.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny went home because he was sick."));
                dMonster.getMap().killFriendlies(dMonster);
                EventInstanceManager eim = dMonster.getEventInstance();
                eim.disbandParty(); // not disbanding -- simply warp all players out removing all party quest items from each player's inventory
            }
            dMonster.getMap().addBunnyHit();
        }
        dMonster.getMap().broadcastMessage(MaplePacketCreator.MobDamageMobFriendly(dMonster, damage), dMonster.getPosition());
        getClient().announce(MaplePacketCreator.enableActions());
    }
}