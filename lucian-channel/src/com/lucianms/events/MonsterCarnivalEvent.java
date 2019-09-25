package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.features.GenericEvent;
import com.lucianms.features.carnival.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkillFactory;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleReactor;
import com.lucianms.server.maps.MapleReactorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.Optional;

/**
 * @author izarooni
 */
public final class MonsterCarnivalEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterCarnivalEvent.class);
    private static final int ID_Reactor = 9980000;
    private static final int MaximumSpawns = 10;

    private byte action;
    private int value;

    public MonsterCarnivalEvent() {
        onPost(new Runnable() {
            @Override
            public void run() {
                getClient().announce(MaplePacketCreator.enableActions());
            }
        });
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        value = reader.readShort();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Optional<GenericEvent> optional = player.getGenericEvents().stream().filter(g -> (g instanceof MCarnivalGame)).findFirst();
        if (optional.isPresent()) {

            MCarnivalGame game = (MCarnivalGame) optional.get();
            final MapleMap map = getClient().getChannelServer().getMap(game.getLobby().getBattlefieldMapId());
            MonsterCarnival carnival = map.getMonsterCarnival();
            MCarnivalTeam friendly = game.getTeam(player.getTeam());
            MCarnivalTeam enemy = game.getTeamOpposite(player.getTeam());

            if (friendly != null && enemy != null) {
                final int nPrice = getPrice(carnival, action, value);
                final int nMonster = getMonster(value);
                if (player.getCarnivalPoints() >= nPrice) {
                    if (action == 0) { // Spawning
                        if (friendly.getSummonedMonsters() <= carnival.getMonsterGenMax()) {
                            if (nPrice == -1) {
                                LOGGER.error("Invalid action/value Action {}, Value {}", action, value);
                                player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 5));
                                return null;
                            } else if (nMonster == -1) {
                                LOGGER.error("Invalid monster value {}", value);
                                player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 5));
                                return null;
                            }
                            MapleMonster monster = MapleLifeFactory.getMonster(nMonster);
                            if (monster != null) {
                                monster.setPosition(new Point(1, 1));
                                monster.setTeam(friendly.getId());
                                map.addCarnivalMonster(monster, friendly.getId());
                                friendly.setSummonedMonsters(friendly.getSummonedMonsters() + 1);

                                player.useCP(nPrice);
                                friendly.setAvailableCarnivalPoints(friendly.getAvailableCarnivalPoints() - nPrice);
                                map.getReactors().stream().filter(r -> r.getId() == (ID_Reactor + enemy.getId())).forEach(r -> r.getMonsterStatus().getRight().applyEffect(null, monster, true));
                                map.broadcastMessage(MCarnivalPacket.getMonsterCarnivalPointsUpdateParty(friendly));
                            } else {
                                LOGGER.error("Unable to summon monster '{}'", getMonster(value));
                            }
                        } else {
                            player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 2));
                            return null;
                        }
                    } else if (action == 1) { // Buffs

                    } else if (action == 2) { // Debuffs
                        if (friendly.getSummonedGuardians() >= carnival.getGuardianGenMax()) {
                            player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 2));
                            return null;
                        }
                        int reactorId = ID_Reactor + friendly.getId();
                        MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
                        reactor.setTeam(friendly.getId());
                        Optional<MCarnivalGuardian> any = carnival.getGuardians().stream()
                                .filter(g -> !carnival.isMapDivded() || g.getTeam() == player.getTeam()).findAny();
                        if (!any.isPresent()) {
                            LOGGER.warn("Failed to summon Monster Carnival guardian {}", reactorId);
                            return null;
                        }
                        MCarnivalGuardian guardian = any.get();
                        reactor.setPosition(guardian.getLocation());
                        switch (value) {
                            case 0:
                                reactor.setMonsterStatus(MonsterStatus.WEAPON_ATTACK_UP, MobSkillFactory.getMobSkill(150, 1));
                                break;
                            case 1:
                                reactor.setMonsterStatus(MonsterStatus.WEAPON_DEFENSE_UP, MobSkillFactory.getMobSkill(151, 1));
                                break;
                            case 2:
                                reactor.setMonsterStatus(MonsterStatus.MAGIC_ATTACK_UP, MobSkillFactory.getMobSkill(152, 1));
                                break;
                            case 3:
                                reactor.setMonsterStatus(MonsterStatus.MAGIC_DEFENSE_UP, MobSkillFactory.getMobSkill(153, 1));
                                break;
                            case 4:
                                reactor.setMonsterStatus(MonsterStatus.ACC, MobSkillFactory.getMobSkill(154, 1));
                                break;
                            case 5:
                                reactor.setMonsterStatus(MonsterStatus.AVOID, MobSkillFactory.getMobSkill(155, 1));
                                break;
                            case 6:
                                reactor.setMonsterStatus(MonsterStatus.SPEED, MobSkillFactory.getMobSkill(156, 1));
                                break;
                            case 7:
                                reactor.setMonsterStatus(MonsterStatus.WEAPON_IMMUNITY, MobSkillFactory.getMobSkill(140, 1));
                                break;
                            case 8:
                                reactor.setMonsterStatus(MonsterStatus.MAGIC_IMMUNITY, MobSkillFactory.getMobSkill(141, 1));
                                break;
                        }
                        map.getMonsters().stream().filter(m -> m.getTeam() == player.getTeam()).forEach(m -> reactor.getMonsterStatus().getRight().applyEffect(null, m, true));
                        map.spawnReactor(reactor);
                        friendly.setSummonedGuardians(friendly.getSummonedGuardians() + 1);
                    }
                    map.broadcastMessage(MCarnivalPacket.getMonsterCarnivalSummon(action, value, player.getName()));
                } else {
                    player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 1));
                }
            } else {
                player.announce(MCarnivalPacket.getMonsterCarnivalResponse((byte) 5));
            }
        }
        return null;
    }

    public int getMonster(int n) {
        return (n >= 0 && n <= 9) ? (9300127 + n) : -1;
    }

    public int getPrice(MonsterCarnival carnival, int action, int n) {
        if (action == 0) {
            MCarnivalMonster monster = carnival.getMonsters().get(n);
            return monster.getSpendCP();
        } else if (action == 1) {
            switch (n) {
                // @formatter:off
                case 1: return 17; // Darkness (Party)
                case 2: return 19; // Weakness (Party)
                case 4: return 12; // Curse (Party)
                case 3: return 19; // Poison (Party)
                case 5: return 16; // Slow (Party)
                case 6: return 14; // Seal (Single)
                case 7: return 22; // Stun (Single)
                case 8: return 18; // Cancel Buffs (Single)
                // @formatter:on
            }
        } else {
            switch (n) {
                // @formatter:off
                case 0: return 17; // Power UP
                case 1: return 16; // Guard UP
                case 2: return 17; // Magic UP
                case 3: return 16; // Shield UP
                case 4: return 13; // Accuracy UP
                case 5: return 16; // Avoidability UP
                case 6: return 12; // Speed UP
                case 7: return 35; // Cancel Weapon
                case 8: return 35; // Cancel Magic
                // @formatter:on
            }
        }
        return -1;
    }
}
