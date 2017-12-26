package net.server.channel.handlers;

import client.MapleCharacter;
import client.status.MonsterStatus;
import net.PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.events.custom.GenericEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleReactor;
import server.maps.MapleReactorFactory;
import server.partyquest.carnival.MCarnivalGame;
import server.partyquest.carnival.MCarnivalTeam;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.Optional;

/**
 * @author izarooni
 */
public final class MonsterCarnivalHandler extends PacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterCarnivalHandler.class);
    private static final int ID_Reactor = 9980000;

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
            final MapleMap map = getClient().getChannelServer().getMapFactory().getMap(game.getLobby().getBattlefieldMapId());
            final MCarnivalTeam friendly = game.getTeam(player.getTeam());
            final MCarnivalTeam enemy = game.getTeamOpposite(player.getTeam());

            if (friendly != null && enemy != null) {
                LOGGER.info("player '{}', Action {}, Value {}", player.getName(), action, value);
                int nReturn = getPrice(action, value);
                if (player.getCarnivalPoints() >= nReturn) {
                    if (action == 0) { // Spawning
                        // todo: maximum 7 summons
                        if (nReturn == -1) {
                            LOGGER.error("Invalid action/value Action {}, Value {}", action, value);
                            player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 5));
                            return null;
                        } else if ((nReturn = getMonster(value)) == -1) {
                            LOGGER.error("Invalid monster value {}", value);
                            player.announce(MaplePacketCreator.getMonsterCarnivalResponse((byte) 5));
                            return null;
                        }
                        MapleMonster monster = MapleLifeFactory.getMonster(nReturn);
                        if (monster != null) {
                            monster.setTeam(enemy.getId());
                            map.getReactors().stream().filter(r -> r.getId() == (ID_Reactor + enemy.getId())).forEach(r -> r.getMonsterStatus().getRight().applyEffect(null, monster, true));
                            map.spawnMonsterOnGroudBelow(monster, new Point(1, 1));
                        } else {
                            LOGGER.error("Unable to summon monster '{}'", getMonster(value));
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
        switch (n) {
            // @formatter:off
            default: return -1;
            case 0: return 9300127;
            case 1: return 9300128;
            case 2: return 9300129;
            case 3: return 9300130;
            case 4: return 9300131;
            case 5: return 9300132;
            case 6: return 9300133;
            case 7: return 9300134;
            case 8: return 9300135;
            case 9: return 9300136;
            // @formatter:on
        }
    }

    public int getPrice(int action, int n) {
        if (action == 0) {
            switch (n) {
                // @formatter:off
                case 1:
                case 2:return 7;
                case 3:
                case 4:return 8;
                case 5:
                case 6:return 9;
                case 7:return 10;
                case 8:return 11;
                case 9:return 12;
                case 10:return 30;
                // @formatter:on
            }
        } else if (action == 1) {
            switch (n) {
                // @formatter:off
                case 1: return 17;
                case 2:
                case 4: return 19;
                case 3: return 12;
                case 5: return 16;
                case 6: return 14;
                case 7: return 22;
                case 8: return 18;
                // @formatter:on
            }
        } else {
            switch (n) {
                // @formatter:off
                case 1:
                case 3: return 17;
                case 2:
                case 4:
                case 6: return 16;
                case 5: return 13;
                case 7: return 12;
                case 8:
                case 9: return 35;
                // @formatter:on
            }
        }
        return -1;
    }
}
