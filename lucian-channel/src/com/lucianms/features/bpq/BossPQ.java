package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.ChangeMapEvent;
import com.lucianms.events.PlayerTakeDamageEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.StringUtil;

import java.awt.*;
import java.util.ArrayList;
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
    private long totalPossibleDamage;
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

        registerAnnotationPacketEvents(this);
    }

    public abstract int getMinimumLevel();

    public abstract Point getMonsterSpawnPoint();

    public abstract void giveRewards(MapleCharacter player);

    public MapleMap getMapInstance(int mapId) {
        MapleMap map = maps.computeIfAbsent(mapId, i -> new FieldBuilder(0, channelID, mapId).loadAll().loadMonsters().build());
        map.setInstanced(true);
        return map;
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
            // 15 is the maximum possible number of attacks for a skill so i'm using 20 to generalize a maximum TOTAL damage
            totalPossibleDamage += player.calculateMaxBaseDamage(player.getTotalWatk() + player.getTotalMagic()) * 20;
        }
    }

    public final void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        int ReturnMap = player.getSavedLocation("OTHER");
        if (ReturnMap == -1) {
            ReturnMap = ServerConstants.HOME_MAP;
        }
        player.changeMap(ReturnMap);
        MapleMap mapInstance = maps.get(mapId);
        if (mapInstance.getAllPlayer().isEmpty()) {
            mapInstance.killAllMonsters();
            mapInstance.clearDrops();
            maps.clear();
            maps = null;
        }
    }

    public final void begin() {
        nextRound();
    }

    private void complete() {
        MapleMap map = getMapInstance(mapId);
        if (map != null) {
            ArrayList<MapleCharacter> chars = new ArrayList<>(map.getCharacters());
            for (MapleCharacter players : chars) {
                giveRewards(players);
                unregisterPlayer(players);
            }
        }
    }

    private void nextRound() {
        final MapleMap mapInstance = getMapInstance(mapId);
        if (round >= bosses.length) {
            mapInstance.startMapEffect("Congrats on defeating all of the bosses!", 5120009);
            mapInstance.broadcastMessage(MaplePacketCreator.serverNotice(6, "You will be warped out momentarily"));
            TaskExecutor.createTask(this::complete, 5000);
        } else {
            broadcastPacket(MaplePacketCreator.getClock(8));
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    mapInstance.killAllMonsters(); // monsters that take too long to die

                    broadcastPacket(MaplePacketCreator.removeClock());
                    int monsterId = bosses[round];
                    MapleMonster monster = MapleLifeFactory.getMonster(monsterId);

                    if (monster != null) {
                        MapleMonsterStats overrides = new MapleMonsterStats();
                        // bracket galore
                        long newHp = (long) ((totalPossibleDamage * getHealthMultiplier()) * (1 + Randomizer.nextDouble(5) * round));
                        if (mapId == 803 && monsterId == 9895253) { // hell mode and last boss (black mage)
                            newHp -= (newHp * 0.25);
                        }
                        overrides.setRevives(null);
                        overrides.setBanishInfo(null);
                        overrides.setHp(newHp);
                        overrides.setTagColor(1);
                        overrides.setTagBgColor(5);
                        overrides.setBoss(true);

                        monster.setOverrideStats(overrides);
                        monster.setBoss(true);
                        final long spawnTimestamp = System.currentTimeMillis();
                        monster.getListeners().add(new MonsterListener() {
                            @Override
                            public void monsterKilled(MapleMonster monster, MapleCharacter player) {
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
                        mapInstance.spawnMonsterOnGroudBelow(monster, getMonsterSpawnPoint());
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
    public void onPlayerHurt(PlayerTakeDamageEvent event) {
        // it's probably a bad idea to have monsters that 1-hit the player and is unavoidable
        // so if that total damage exceeds the player's health, just set the damage amount to (current_hp - 1)
        int nDamage = Math.min(event.getClient().getPlayer().getHp() - 1, (int) (event.getDamage() * getDamageMultiplier()));

        if (mapId == 803 && bosses[round] == 9895253) { // hell mode and last boss (black mage)
            nDamage -= (nDamage * 0.25);
        }
        event.setDamage(nDamage);
    }

    @Override
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        if (sender instanceof ChangeMapEvent) {
            player.setHp(player.getMaxHp());
            player.changeMap(player.getMap());
            return true;
        }
        return super.onPlayerDeath(sender, player);
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        unregisterPlayer(player);
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (!destination.isInstanced()) {
            unregisterPlayer(player);
            return false;
        }
        return true;
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
