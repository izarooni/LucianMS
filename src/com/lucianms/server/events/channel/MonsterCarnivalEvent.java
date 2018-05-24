package com.lucianms.server.events.channel;

import client.MapleCharacter;
import client.status.MonsterStatus;
import com.lucianms.features.GenericEvent;
import com.lucianms.server.pqs.carnival.MCarnivalGame;
import com.lucianms.server.pqs.carnival.MCarnivalTeam;
import net.PacketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleReactorFactory;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

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

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        action = slea.readByte();
        value = slea.readShort();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Optional<GenericEvent> optional = player.getGenericEvents().stream().filter(g -> (g instanceof MCarnivalGame)).findFirst();
        if (optional.isPresent()) {

            MCarnivalGame game = (MCarnivalGame) optional.get();
            final MapleMap map = getClient().getChannelServer().getMap(game.getLobby().getBattlefieldMapId());
            final MCarnivalTeam friendly = game.getTeam(player.getTeam());
            final MCarnivalTeam enemy = game.getTeamOpposite(player.getTeam());

            if (friendly != null && enemy != null) {
                LOGGER.info("player '{}', Action {}, Value {}", player.getName(), action, value);
                final int nPrice = getPrice(action, value);
                final int nMonster = getMonster(value);
                if (player.getCarnivalPoints() >= nPrice) {
                    if (action == 0) { // Spawning
                        if (friendly.getSummonedMonsters() <= MaximumSpawns) {
                            if (nPrice == -1) {
                                LOGGER.error("Invalid action/value Action {}, Value {}", action, value);
                                player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 5));
                                return null;
                            } else if (nMonster == -1) {
                                LOGGER.error("Invalid monster value {}", value);
                                player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 5));
                                return null;
                            }
                            MapleMonster monster = MapleLifeFactory.getMonster(nMonster);
                            if (monster != null) {
                                monster.setPosition(new Point(1, 1));
                                monster.setTeam(friendly.getId());
                                map.addCarnivalMonster(monster, friendly);
                                friendly.setSummonedMonsters(friendly.getSummonedMonsters() + 1);

                                player.useCP(nPrice);
                                friendly.setAvailableCarnivalPoints(friendly.getAvailableCarnivalPoints() - nPrice);
                                map.getReactors().stream().filter(r -> r.getId() == (ID_Reactor + enemy.getId())).forEach(r -> r.getMonsterStatus().getRight().applyEffect(null, monster, true));
                                map.broadcastMessage(MaplePacketCreator.getMonsterCarnivalPointsUpdateParty(friendly));
                            } else {
                                LOGGER.error("Unable to summon monster '{}'", getMonster(value));
                            }
                        } else {
                            player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 2));
                        }
                    } else if (action == 1) { // Buffs

                    } else if (action == 2) { // Debuffs
                        int reactorId = ID_Reactor + friendly.getId();
                        MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(reactorId), reactorId);
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
                        player.getMap().spawnReactor(reactor);
                    }
                } else {
                    player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 1));
                }
            } else {
                player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 5));
            }
        }
        return null;
    }

    @Override
    public void post() {
        // is this necessary?
        getClient().announce(MaplePacketCreator.enableActions());
    }

    public int getMonster(int n) {
        return (n >= 0 && n <= 9) ? (9300127 + n) : -1;
    }

    public int getPrice(int action, int n) {
        if (action == 0) {
            switch (n) {
                // @formatter:off
                case 0: return 7; // Brown Teddy
                case 1: return 7; // Blocotpus
                case 2: return 8; // Ratz
                case 3: return 8; // Chronos
                case 4: return 9; // Toy Trojan
                case 5: return 9; // Tick-Tock
                case 6: return 10; // Robo
                case 7: return 11; // King Block Golem
                case 8: return 12; // Master Chronos
                case 9: return 30; // Rombots
                // @formatter:on
            }
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
