package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SkillFactory;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleSummon;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerSummonTakeDamageEvent extends PacketEvent {

    private byte unk1;
    private int skillID;
    private int damage;
    private int monsterID;

    @Override
    public void processInput(MaplePacketReader reader) {
        skillID = reader.readInt();
        unk1 = reader.readByte();
        damage = reader.readInt();
        monsterID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        if (SkillFactory.getSkill(skillID) != null) {
            MapleCharacter player = getClient().getPlayer();
            MapleSummon summon = player.getSummons().get(skillID);
            if (summon != null) {
                summon.addHP(-damage);
                if (summon.getHP() <= 0) {
                    // dispose
                }
                player.getMap().broadcastMessage(player, MaplePacketCreator.damageSummon(player.getId(), skillID, damage, unk1, monsterID), summon.getPosition());
            }
        }
        return null;
    }
}
