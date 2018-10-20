package net.server.channel.handlers;

import client.MapleCharacter;
import client.Skill;
import client.SkillFactory;
import client.status.MonsterStatusEffect;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.maps.MapleSummon;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 * @author izarooni
 */
public class SummonDealDamageEvent extends PacketEvent {

    private byte direction;
    private int countAttcked;
    private int objectID;
    private Pair<Integer, Integer>[] attackEntries; // monsterID, damage

    @Override
    public void clean() {
        attackEntries = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        objectID = reader.readInt();
        reader.skip(4);
        direction = reader.readByte();
        countAttcked = reader.readByte();
        reader.skip(8);
        attackEntries = new Pair[countAttcked];
        for (int i = 0; i < countAttcked; i++) {
            int monsterID = reader.readInt();
            reader.skip(18);
            int damage = reader.readInt();
            attackEntries[i] = new Pair<>(monsterID, damage);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!player.isAlive()) {
            return null;
        }
        MapleSummon summon = player.getSummons().values().stream().filter(s -> s.getObjectId() == objectID).findFirst().orElse(null);
        if (summon == null) {
            return null;
        }
        Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        player.getMap().broadcastMessage(player, MaplePacketCreator.summonAttack(player.getId(), summon.getSkill(), direction, attackEntries), summon.getPosition());
        for (Pair<Integer, Integer> pairs : attackEntries) {
            int damage = pairs.getRight();
            MapleMonster target = player.getMap().getMonsterByOid(pairs.getLeft());
            if (target != null) {
                if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
                    if (summonEffect.makeChanceResult()) {
                        target.applyStatus(player, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, null, false), summonEffect.isPoison(), 4000);
                    }
                }
                player.getMap().damageMonster(player, target, damage);
            }
        }
        return null;
    }
}