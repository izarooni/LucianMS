package com.lucianms.features.dungeon;

import client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import constants.ExpTable;
import constants.ServerConstants;
import net.server.world.MaplePartyCharacter;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.Randomizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A dungeon builder.
 * @TODO test functionality to make sure it works.
 * @author Lucas
 * @version 0.1
 */
public class DungeonBuilder {

    private MapleCharacter player;

    private int timeLimit = 300;
    private boolean scaleEXP = false;
    private ArrayList<MapleMonster> spawns = new ArrayList<>();
    private boolean allowParty = false;
    private  int maxPartySize = 6;
    private int monsterExp = 1337; // this won't be used if scaleEXP is not on.

    private int scaleFromTotal = 1000; // kill 1000 monsters to reach level up

    private int minLevel = 0, maxLevel = 255;
    private ArrayList<Integer> itemRequirements = new ArrayList<>();

    private Task respawnTask, endTask;

    private boolean everyoneNeedsItemRequirements = false;

    private int mapId = 0, returnMap = 0;

    private int monstersPerPoint = 1;

    private int minX = 0, maxY = 0;

    public DungeonBuilder(MapleCharacter player, int mapId) {
        this.player = player;
        this.mapId = mapId;
    }


    // lol for now
    public static DungeonBuilder prepare(MapleCharacter player, int mapId) {
        return new DungeonBuilder(player, mapId);
    }

    /**
     * Attach item requirements to enter.
     *
     * @param items
     * @return DungeonBuilder
     */
    public DungeonBuilder attachRequirements(Integer... items) {
        itemRequirements.addAll(Arrays.asList(items));
        return this;
    }

    /**
     * Attach all spawns to the dungeon.
     *
     * @param monsters integer[] (kind of)
     * @return DungeonBuilder
     */
    public DungeonBuilder attachSpawns(Integer... monsters) {
        if (scaleEXP) {
            Arrays.asList(monsters).forEach((integer) -> {
                MapleMonster monster = MapleLifeFactory.getMonster(integer);
                if (monster != null) {
                    MapleCharacter expCalc = player;
                    if (player.getParty() != null) {
                        for (MaplePartyCharacter expCalculation : player.getParty().getMembers()) {
                            if (expCalculation.getLevel() < expCalc.getLevel()) {
                                expCalc = expCalculation.getPlayer();
                            }
                        }
                    }
                    monster.getStats().setExp(ExpTable.getExpNeededForLevel(expCalc.getLevel()) / scaleFromTotal);
                    monster.getStats().setHp(monster.getStats().getHp() * ((expCalc.getLevel() / 20) * 5));
                    spawns.add(monster);
                }
            });
        } else {
            Arrays.asList(monsters).forEach((integer) -> {
                MapleMonster monster = MapleLifeFactory.getMonster(integer);
                if (monster != null) {
                    monster.getStats().setExp(this.monsterExp);
                    spawns.add(monster);
                }
            });
        }
        return this;
    }


    // TODO item requirements
    private boolean buildDungeon(boolean isPartyPlay) {
        MapleMapFactory factory = new MapleMapFactory(player.getWorld(), player.getClient().getChannel());
        MapleMap map = factory.getMap(this.mapId);
        if(map != null) {

            spawns.forEach((monster) -> {
                monster.setPosition(map.getGroundBelow(new Point(Randomizer.nextInt(minX - maxY) + minX, Randomizer.nextInt(maxY))));
                map.addMonsterSpawn(monster, 3000, -1);
            });

            // Don't want em staying there OwO
            map.setReturnMapId(ServerConstants.HOME_MAP);
            map.setForcedReturnMap(ServerConstants.HOME_MAP);
            // don't want portals to work nor scripts
            map.getPortals().forEach((portal) -> { portal.setPortalStatus(false); portal.setScriptName(null); });

            if(isPartyPlay) {
                if(player.getParty() != null) {
                    player.getParty().getMembers().forEach((member) -> member.getPlayer().changeMap(map));
                } else {
                    player.changeMap(map);
                }
            } else {
                player.changeMap(map);
            }

            // A respawning task that'll end the dungeon if everyone is gone
            respawnTask = TaskExecutor.createRepeatingTask(() -> {
                if(map.getAllPlayer().size() < 1) {
                    endTask.cancel();
                    respawnTask.cancel();
                } else {
                    map.respawn();
                }
            }, 3000);

            // get these people out of here!!
            endTask = TaskExecutor.createTask(() -> {
                map.getAllPlayer().forEach((move) -> move.changeMap(this.returnMap));
                if(!respawnTask.isCanceled()) {
                    respawnTask.cancel();
                }
            }, timeLimit * 1000);

            return true;
        }
        return false;
    }

    public boolean enter() {
        if (this.allowParty) {
            if (!(spawns.isEmpty() && itemRequirements.isEmpty())) {
                if (player.getParty() != null) {
                    if (player.getParty().getMembers().size() <= maxPartySize) {
                        return this.buildDungeon(true);
                    }
                }
            }
        } else {
            return this.buildDungeon(false);
        }

        return false;
    }


    public DungeonBuilder setAllowParty(boolean allowParty) {
        this.allowParty = allowParty;
        return this;
    }

    public DungeonBuilder setEveryoneNeedsItemRequirements(boolean everyoneNeedsItemRequirements) {
        this.everyoneNeedsItemRequirements = everyoneNeedsItemRequirements;
        return this;
    }

    public DungeonBuilder setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    public DungeonBuilder setMaxPartySize(int maxPartySize) {
        this.maxPartySize = maxPartySize;
        return this;
    }

    public DungeonBuilder setMinLevel(int minLevel) {
        this.minLevel = minLevel;
        return this;
    }

    public DungeonBuilder setMonsterExp(int monsterExp) {
        this.monsterExp = monsterExp;
        return this;
    }

    public DungeonBuilder setEndTask(Task endTask) {
        this.endTask = endTask;
        return this;
    }

    public DungeonBuilder setRespawnTask(Task respawnTask) {
        this.respawnTask = respawnTask;
        return this;
    }

    public DungeonBuilder setScaleEXP(boolean scaleEXP) {
        this.scaleEXP = scaleEXP;
        return this;
    }

    public DungeonBuilder setScaleFromTotal(int scaleFromTotal) {
        this.scaleFromTotal = scaleFromTotal;
        return this;
    }

    public DungeonBuilder setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
        return this;
    }

    public DungeonBuilder setReturnMap(int returnMap) {
        this.returnMap = returnMap;
        return this;
    }

    public DungeonBuilder setMonstersPerPoint(int number) {
        this.monstersPerPoint = number;
        return this;
    }

    public int getMapId() {
        return mapId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getMaxPartySize() {
        return maxPartySize;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMonsterExp() {
        return monsterExp;
    }

    public int getMonstersPerPoint() {
        return monstersPerPoint;
    }

    public int getReturnMap() {
        return returnMap;
    }

    public int getScaleFromTotal() {
        return scaleFromTotal;
    }

    public Task getEndTask() {
        return endTask;
    }

    public Task getRespawnTask() {
        return respawnTask;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public ArrayList<Integer> getItemRequirements() {
        return itemRequirements;
    }

    public ArrayList<MapleMonster> getSpawns() {
        return spawns;
    }

    public DungeonBuilder setDimensions(int minX, int maxY) {
        this.minX = minX;
        this.maxY = maxY;
        return this;
    }

    public boolean isEveryoneNeedsItemRequirements() {
        return everyoneNeedsItemRequirements;
    }
}
