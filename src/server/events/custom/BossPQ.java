package server.events.custom;

import client.MapleCharacter;
import net.server.handlers.PlayerDisconnectHandler;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.annotation.PacketWorker;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BossPQ extends GenericEvent {

    private static final int ReturnMap = 240070101;

    private final int mapId;
    private final int[] bosses;
    private final MapleMapFactory mapFactory;

    private int round = 0;
    private int points = 0;
    private int nCashWinnings = 0;
    private int nCashMultiplier = 1;
    private int mHealthMultiplier = 1;
    private int mDamageMultiplier = 1;

    public BossPQ(int channel, int mapId, int[] bosses) {
        this.mapId = mapId;
        this.bosses = bosses;

        mapFactory = new MapleMapFactory(0, channel);
    }

    private MapleMap getMapInstance(int mapId) {
        return mapFactory.skipMonsters(true).getMap(mapId);
    }

    private void broadcastPacket(byte[] packet) {
        getMapInstance(mapId).broadcastMessage(packet);
    }

    private void broadcastMessage(String message) {
        getMapInstance(mapId).broadcastMessage(MaplePacketCreator.serverNotice(6, "[BossPQ] " + message));
    }

    public final void registerPlayer(MapleCharacter player) {
        player.changeMap(getMapInstance(mapId));
        player.addGenericEvent(this);
    }

    public final void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(ReturnMap);
    }

    public final void registerParty(MapleParty party) {
        MapleCharacter leader = party.getLeader().getPlayer();
        MapleMap map = getMapInstance(mapId);
        if (map != null) {
            for (MaplePartyCharacter members : party.getMembers()) {
                if (members.getMapId() == leader.getMapId() && members.isOnline()) {
                    members.getPlayer().changeMap(map);
                    members.getPlayer().addGenericEvent(this);
                }
            }
        } else {
            leader.dropMessage(5, "An error occurred while trying to warp your party");
        }
    }

    public final void begin() {
        nextRound();
    }

    private void complete() {
        MapleMap map = getMapInstance(mapId);
        if (map != null) {
            for (MapleCharacter players : map.getCharacters()) {
                unregisterPlayer(players);
            }
        }
    }

    private void nextRound() {
        if (round >= bosses.length) {
            complete();
        } else {
            final int bRoundId = round;
            broadcastPacket(MaplePacketCreator.getClock(8));
            createTask(new Runnable() {
                @Override
                public void run() {
                    int monsterId = bosses[round];
                    MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
                    if (monster != null) {
                        MapleMap map = getMapInstance(mapId);
                        if (map != null) {
                            MapleMonsterStats stats = new MapleMonsterStats();
                            stats.setHp(monster.getHp() * getHealthMultiplier());
                            stats.setMp(monster.getMp() * getHealthMultiplier());
                            monster.setBoss(true);
                            final long spawnTimestamp = System.currentTimeMillis();
                            monster.addListener(new MonsterListener() {
                                @Override
                                public void monsterKilled(int aniTime) {
                                    long endTime = System.currentTimeMillis();
                                    int gain = (getCashWinnings() * getnCashMultiplier());
                                    broadcastMessage(String.format("Round %d completed! It took you %s to kill that boss", bRoundId, StringUtil.getTimeElapse(endTime - spawnTimestamp)));
                                    for (MapleCharacter players : map.getCharacters()) {
                                        players.dropMessage(6, "You gained " + StringUtil.formatNumber(gain) + " NX for completing this round");
                                    }
                                    nextRound();
                                }
                            });
                            map.spawnMonsterOnGroudBelow(monster, new Point(-28, 181));
                        }
                    }
                }
            }, 8000);
        }
        round++;
    }

    @PacketWorker
    public void onPlayerDisconnect(PlayerDisconnectHandler event) {
        unregisterPlayer(event.getClient().getPlayer());
    }

    public int getMapId() {
        return mapId;
    }

    public int[] getBosses() {
        return bosses.clone();
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCashWinnings() {
        return nCashWinnings;
    }

    public void setCashWinnings(int nCashWinnings) {
        this.nCashWinnings = nCashWinnings;
    }

    public int getnCashMultiplier() {
        return nCashMultiplier;
    }

    public void setnCashMultiplier(int nCashMultiplier) {
        this.nCashMultiplier = nCashMultiplier;
    }

    public int getHealthMultiplier() {
        return mHealthMultiplier;
    }

    public void setHealthMultiplier(int mHealthMultiplier) {
        this.mHealthMultiplier = mHealthMultiplier;
    }

    public int getDamageMultiplier() {
        return mDamageMultiplier;
    }

    public void setDamageMultiplier(int mDamageMultiplier) {
        this.mDamageMultiplier = mDamageMultiplier;
    }
}
