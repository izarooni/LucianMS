package com.lucianms.features.bpq;

import client.MapleCharacter;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.events.channel.TakeDamageEvent;
import server.FieldBuilder;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.awt.*;
import java.util.HashMap;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public abstract class BossPQ extends GenericEvent {

    private final int channelID;
    private final int mapId;
    private final int[] bosses;

    private HashMap<Integer, MapleMap> maps = new HashMap<>(5);
    private int round = 0;
    private int points = 0;
    private int nCashWinnings = 0; // how much NX is gained per round
    private float nCashMultiplier = 1.0f; // multiply winnings with this
    private float mHealthMultiplier = 1.0f; // multiply each boss health with this
    private float mDamageMultiplier = 1.0f; // multiply damage taken with this

    public BossPQ(int channelID, int mapId, int[] bosses) {
        this.channelID = channelID;
        this.mapId = mapId;
        this.bosses = bosses;
    }

    public abstract int getMinimumLevel();

    public abstract Point getMonsterSpawnPoint();

    public abstract void giveRewards(MapleCharacter player);

    public MapleMap getMapInstance(int mapId) {
        return maps.getOrDefault(mapId, new FieldBuilder(0, channelID, mapId).loadAll().loadMonsters().build());
    }

    private void broadcastPacket(byte[] packet) {
        getMapInstance(mapId).broadcastMessage(packet);
    }

    public void broadcastMessage(String message) {
        getMapInstance(mapId).broadcastMessage(MaplePacketCreator.serverNotice(6, "[BossPQ] " + message));
    }

    public final void registerPlayer(MapleCharacter player) {
        if (player.addGenericEvent(this)) {
            player.saveLocation("OTHER");
            player.changeMap(getMapInstance(mapId));
        }
    }

    public final void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        int ReturnMap = player.getSavedLocation("OTHER");
        if (ReturnMap == -1) {
            ReturnMap = 240070101;
        }
        player.changeMap(ReturnMap);
        maps.clear();
        maps = null;
    }

    public final void begin() {
        nextRound();
    }

    private void complete() {
        MapleMap map = getMapInstance(mapId);
        if (map != null) {
            for (MapleCharacter players : map.getCharacters()) {
                giveRewards(players);
                unregisterPlayer(players);
            }
        }
    }

    private void nextRound() {
        if (round >= bosses.length) {
            getMapInstance(mapId).startMapEffect("Congrats on defeating all of the bosses!", 5120009);
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    complete();
                }
            }, 4000);
        } else {
            broadcastPacket(MaplePacketCreator.getClock(8));
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    int monsterId = bosses[round];
                    MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
                    if (monster != null) {
                        MapleMap map = getMapInstance(mapId);
                        if (map != null) {
                            MapleMonsterStats stats = new MapleMonsterStats();
                            int newHp = (int) (monster.getHp() * getHealthMultiplier());
                            int newMp = (int) (monster.getMp() * getHealthMultiplier());
                            if (newHp < 1) {
                                // number overflow
                                newHp = Integer.MAX_VALUE;
                            }
                            if (mapId == 803 && monsterId == 9895253) { // hell mode and last boss (black mage)
                                newHp -= (newHp * 0.25);
                                newMp -= (newMp * 0.25);
                            }
                            stats.setHp(newHp);
                            stats.setMp(newMp);
                            monster.setBoss(true);
                            final long spawnTimestamp = System.currentTimeMillis();
                            monster.addListener(new MonsterListener() {
                                @Override
                                public void monsterKilled(int aniTime) {
                                    long endTime = System.currentTimeMillis();
                                    long elapse = endTime - spawnTimestamp;
                                    String time;
                                    if (elapse < 1000) {
                                        time = ((elapse) / 1000d) + "s";
                                    } else {
                                        time = StringUtil.getTimeElapse(elapse);
                                    }
                                    broadcastMessage(String.format("Round %d completed! It took you %s to kill that boss", (round + 1), time));

                                    round++;
                                    nextRound();
                                }
                            });
                            map.spawnMonsterOnGroudBelow(monster, getMonsterSpawnPoint());
                        }
                    }
                }
            }, 8000);
        }
    }

    @Override
    public boolean banishPlayer(MapleCharacter player, int mapId) {
        return false;
    }

    @PacketWorker
    public void onPlayerHurt(TakeDamageEvent event) {
        // it's probably a bad idea to have monsters that 1-hit the player and is unavoidable
        // so if that total damage exceeds the player's health, just set the damage amount to (current_hp - 1)
        int nDamage = Math.min(event.getClient().getPlayer().getHp() - 1, (int) (event.getDamage() * getDamageMultiplier()));

        if (mapId == 803 && bosses[round] == 9895253) { // hell mode and last boss (black mage)
            nDamage -= (nDamage * 0.25);
        }
        event.setDamage(nDamage);
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        unregisterPlayer(player);
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

    public float getCashMultiplier() {
        return nCashMultiplier;
    }

    public void setCashMultiplier(float nCashMultiplier) {
        this.nCashMultiplier = nCashMultiplier;
    }

    public float getHealthMultiplier() {
        return mHealthMultiplier;
    }

    public void setHealthMultiplier(float mHealthMultiplier) {
        this.mHealthMultiplier = mHealthMultiplier;
    }

    public float getDamageMultiplier() {
        return mDamageMultiplier;
    }

    public void setDamageMultiplier(float mDamageMultiplier) {
        this.mDamageMultiplier = mDamageMultiplier;
    }
}
