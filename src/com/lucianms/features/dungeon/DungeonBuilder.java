package com.lucianms.features.dungeon;

import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import constants.ExpTable;
import constants.ServerConstants;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import server.MapleInventoryManipulator;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


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

    private int minX = 0, maxX = 0;

    private int[] platforms;

    private int maxMonsterAmount;

    private int respawnTime = 4;

    private String areLacking = "";

    private boolean disableDrops = true;

    private DungeonBuilder(MapleCharacter player, int mapId) {
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
     * @param items all the required items
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
            System.out.println("[Dungeon Builder] Scaling EXP for monsters");
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
                    monster.getStats().setExp(ExpTable.getExpNeededForLevel(expCalc.getLevel()) / getScaleFromTotal());
                    monster.getStats().setHp(expCalc.getHp() * 2);
                    monster.setLevel(expCalc.getLevel());
                    System.out.println(String.format("[Dungeon Builder] EXP for monster: %d, EXP %d", monster.getMaxHp(), monster.getStats().getExp()));
                    spawns.add(monster);
                }
            });
            System.out.println("[Dungeon Builder] Scaling complete.");
        } else {
            System.out.println("[Dungeon Builder] Scaling is not enabled.");
            Arrays.asList(monsters).forEach((integer) -> {
                MapleMonster monster = MapleLifeFactory.getMonster(integer);
                if (monster != null) {
                    monster.getStats().setExp(getMonsterExp());
                    spawns.add(monster);
                }
            });
        }
        return this;
    }


    // TODO item requirements
    private boolean buildDungeon(boolean isPartyPlay) {
        if(allowEntrance()) {
            MapleMapFactory factory = new MapleMapFactory(player.getWorld(), player.getClient().getChannel());
            MapleMap map = factory.getMap(this.mapId);
            if (map != null) {

                // Don't want em staying there OwO
                map.setReturnMapId(ServerConstants.HOME_MAP);
                map.setForcedReturnMap(ServerConstants.HOME_MAP);
                // don't want portals to work nor scripts
                map.getPortals().forEach((portal) -> {
                    portal.setPortalStatus(false);
                    portal.setScriptName(null);
                });

                if(isDisableDrops()) {
                    map.toggleDrops();
                }

                if (isPartyPlay) {
                    if (player.getParty() != null) {
                        player.getParty().getMembers().forEach((member) -> member.getPlayer().changeMap(map));
                    } else {
                        player.changeMap(map);
                    }
                } else {
                    player.changeMap(map);
                }

                // A respawning task that'll end the dungeon if everyone is gone
                respawnTask = TaskExecutor.createRepeatingTask(() -> {
                    if (map.getAllPlayer().size() < 1) {
                        endTask.cancel();
                        respawnTask.cancel();
                    } else {
                        spawns.forEach((monster) -> {
                            int randomPlatform = this.platforms[Randomizer.nextInt(this.platforms.length)];
                            monster.setPosition(map.getGroundBelow(new Point(Randomizer.nextInt(maxX + minX) - minX, randomPlatform)));
                            if (map.getMonsters().size() + this.getMonstersPerPoint() < this.maxMonsterAmount)
                                for (int i = 0; i < this.getMonstersPerPoint(); i++) {
                                    MapleMonster nMonster = MapleLifeFactory.getMonster(monster.getId());
                                    nMonster.getStats().setExp(monster.getStats().getExp());
                                    nMonster.setHp(monster.getMaxHp());
                                    nMonster.setLevel(monster.getLevel());
                                    map.spawnMonsterOnGroundBelow(nMonster, monster.getPosition());
                                }
                        });
                    }
                }, getRespawnTime() * 1000);
                map.broadcastMessage(MaplePacketCreator.getClock(getTimeLimit()));
                // get these people out of here!!
                endTask = TaskExecutor.createTask(() -> {
                    map.getAllPlayer().forEach((move) -> move.changeMap(this.returnMap));
                    if (!respawnTask.isCanceled()) {
                        respawnTask.cancel();
                    }
                }, getTimeLimit() * 1000);

                return true;
            }
        }
        return false;
    }

    private boolean allowEntrance() {
        if(player.getParty() != null) {
            for(MaplePartyCharacter pcharacter : player.getParty().getMembers()) {
                MapleCharacter character = pcharacter.getPlayer();
                if(!(character.getLevel() >= getMinLevel() && character.getLevel() <= getMaxLevel())) {
                    areLacking += String.format("%s does not fullfill the level requirements\r\n", character.getName());
                    return false;
                }
                if(isEveryoneNeedsItemRequirements()) {
                    boolean allHave = true;
                    for (Integer integer : getItemRequirements()) {
                        if (!(character.getItemQuantity(integer, false) >= 1)) {
                            allHave = false;
                        }

                        if (!allHave) {
                            areLacking += String.format("%s does not fullfill the item requirements\r\n", character.getName());
                            return false;
                        }
                    }
                } else {
                    for(Integer integer : getItemRequirements()) {
                        if(!(player.getItemQuantity(integer, false) >= 1)) {
                            areLacking += String.format("%s does not fullfill the item requirements\r\n", player.getName());
                            return false;
                        }
                    }
                }
            }
        } else {
            if(!(player.getLevel() >= getMinLevel() && player.getLevel() <= getMaxLevel())) {
                areLacking += String.format("%s does not fullfill the level requirements\r\n", player.getName());
                return false;
            }
            for(Integer integer : getItemRequirements()) {
                if(!(player.getItemQuantity(integer, false) >= 1)) {
                    areLacking += String.format("%s does not fullfill the item requirements\r\n", player.getName());
                    return false;
                }
            }

        }

        if(player.getParty() != null) {
            if(isEveryoneNeedsItemRequirements()) {
                for (MaplePartyCharacter pchar : player.getParty().getMembers()) {
                    MapleCharacter character = pchar.getPlayer();
                    for (Integer integer : getItemRequirements()) {
                        MapleInventoryManipulator.removeById(character.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
                    }
                }
            } else {
                for(Integer integer : getItemRequirements()) {
                    MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
                }
            }
        } else {
            for(Integer integer : getItemRequirements()) {
                MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
            }
        }

        return true;
    }

    public boolean enter() {
        if (this.allowParty) {
            if (!(spawns.isEmpty() && getItemRequirements().isEmpty())) {
                if (player.getParty() != null) {
                    if (player.getParty().getMembers().size() <= getMaxPartySize()) {
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

    public DungeonBuilder setMaxMonsterAmount(int amount) {
        this.maxMonsterAmount = amount;
        return this;
    }

    public DungeonBuilder setDimensions(int minX, int maxX, int[] plats) {
        this.minX = minX;
        this.maxX = maxX;
        this.platforms = plats;
        return this;
    }

    public DungeonBuilder setDisableDrops(boolean disableDrops) {
        this.disableDrops = disableDrops;
        return this;
    }

    public DungeonBuilder setRespawnTime(int respawnTime) {
        this.respawnTime = respawnTime;
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

    public boolean isEveryoneNeedsItemRequirements() {
        return everyoneNeedsItemRequirements;
    }

    public String getAreLacking() {
        return areLacking;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public boolean isDisableDrops() {
        return disableDrops;
    }
}
