package com.lucianms.features.dungeon;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ExpTable;
import com.lucianms.constants.ServerConstants;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.SpawnPoint;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A dungeon builder.
 *
 * @author Lucas
 * @version 0.1
 */
public class DungeonBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonBuilder.class);

    private MapleCharacter player;
    private ArrayList<MapleMonster> spawns = new ArrayList<>();
    private ArrayList<Integer> itemRequirements = new ArrayList<>();
    private Task endTask;
    private Task respawnTask;
    private MapleMap map;
    private String areLacking = "";
    private int timeLimit = 300;
    private int maxPartySize = 1;
    private int monsterExp = 1337; // this won't be used if scaleEXP is not on.
    private int scaleFromTotal = 25; // kill 25 monsters to reach level up
    private int minLevel = 0, maxLevel = 255;
    private int mapId = 599000002, returnMap = 809;
    private int monstersPerPoint = 1;
    private int minX = 0, maxX = 0;
    private int[] platforms;
    private int maxMonsterAmount;
    private int respawnTime = 4;
    private boolean disableDrops = true;
    private boolean everyoneNeedsItemRequirements = false;
    private boolean allowParty = false;
    private boolean scaleEXP = false;

    private DungeonBuilder(MapleCharacter player, int mapId) {
        this.player = player;
        this.mapId = mapId;

        map = new FieldBuilder(player.getWorld(), player.getClient().getChannel(), mapId).loadPortals().loadFootholds().build();
        map.setRespawnEnabled(true);
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
        for (Integer monsterId : monsters) {
            MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
            if (monster != null) {

                int randomPlatform = this.platforms[Randomizer.nextInt(this.platforms.length)];
                Point point = new Point(Randomizer.nextInt(maxX + minX) - minX, randomPlatform - 2);
                monster.setPosition(point);
                SpawnPoint spawnPoint = new SpawnPoint(map, monster, !monster.isMobile(), respawnTime, -1);

                MapleMonsterStats overrides = spawnPoint.createOverrides();
                MapleCharacter expCalc = player;
                if (scaleEXP) {
                    MapleParty party = player.getParty();
                    if (party != null) {
                        for (MaplePartyCharacter expCalculation : party.values()) {
                            if (expCalculation.getLevel() < expCalc.getLevel()) {
                                expCalc = expCalculation.getPlayer();
                            }
                        }
                    }
                }
                overrides.setExp(ExpTable.getExpNeededForLevel(expCalc.getLevel()) / getScaleFromTotal());
                overrides.setHp(expCalc.getHp() * 2);
                monster.setLevel(expCalc.getLevel());
                overrides.setLevel(expCalc.getLevel());

                spawnPoint.summonMonster();
                map.addMonsterSpawnPoint(spawnPoint);
            }
        }
        return this;
    }

    // TODO item requirements
    private boolean buildDungeon(boolean isPartyPlay) {
        if (allowEntrance()) {
            if (map != null) {

                // Don't want em staying there OwO
                map.setReturnMapId(ServerConstants.MAPS.Home);
                map.setForcedReturnMap(ServerConstants.MAPS.Home);
                // don't want portals to work nor scripts
                map.getPortals().forEach((portal) -> {
                    portal.setPortalStatus(false);
                    portal.setScriptName(null);
                });

                if (isDisableDrops()) {
                    map.toggleDrops();
                }

                if (isPartyPlay) {
                    if (player.getParty() != null) {
                        player.getParty().forEachPlayer(p -> p.changeMap(map));
                    } else {
                        player.changeMap(map);
                    }
                } else {
                    player.changeMap(map);
                }

                // A respawning task that'll end the dungeon if everyone is gone
                map.broadcastMessage(MaplePacketCreator.getClock(getTimeLimit()));
                // get these people out of here!!
                endTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        endTask = TaskExecutor.cancelTask(respawnTask);
                        map.getAllPlayer().forEach((move) -> move.changeMap(DungeonBuilder.this.returnMap));
                    }
                }, getTimeLimit() * 1000);
                respawnTask = TaskExecutor.createRepeatingTask(new Runnable() {
                    @Override
                    public void run() {
                        for (SpawnPoint sp : map.getMonsterSpawnPoints()) {
                            sp.attemptMonsterSummon();
                        }
                    }
                }, 3000, 3000);
                return true;
            }
        }
        return false;
    }

    private boolean allowEntrance() {
        if (player.getParty() != null) {
            for (MaplePartyCharacter pcharacter : player.getParty().values()) {
                MapleCharacter character = pcharacter.getPlayer();
                if (!(character.getLevel() >= getMinLevel() && character.getLevel() <= getMaxLevel())) {
                    areLacking += String.format("%s does not fulfill the level requirements\r\n", character.getName());
                    return false;
                }
                if (isEveryoneNeedsItemRequirements()) {
                    boolean allHave = true;
                    for (Integer integer : getItemRequirements()) {
                        if (!(character.getItemQuantity(integer, false) >= 1)) {
                            allHave = false;
                        }

                        if (!allHave) {
                            areLacking += String.format("%s does not fulfill the item requirements\r\n", character.getName());
                            return false;
                        }
                    }
                } else {
                    for (Integer integer : getItemRequirements()) {
                        if (!(player.getItemQuantity(integer, false) >= 1)) {
                            areLacking += String.format("%s does not fulfill the item requirements\r\n", player.getName());
                            return false;
                        }
                    }
                }
            }
        } else {
            if (!(player.getLevel() >= getMinLevel() && player.getLevel() <= getMaxLevel())) {
                areLacking += String.format("%s does not fulfill the level requirements\r\n", player.getName());
                return false;
            }
            for (Integer integer : getItemRequirements()) {
                if (!(player.getItemQuantity(integer, false) >= 1)) {
                    areLacking += String.format("%s does not fulfill the item requirements\r\n", player.getName());
                    return false;
                }
            }

        }

        if (player.getParty() != null) {
            if (isEveryoneNeedsItemRequirements()) {
                for (MaplePartyCharacter pchar : player.getParty().values()) {
                    MapleCharacter character = pchar.getPlayer();
                    for (Integer integer : getItemRequirements()) {
                        MapleInventoryManipulator.removeById(character.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
                    }
                }
            } else {
                for (Integer integer : getItemRequirements()) {
                    MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
                }
            }
        } else {
            for (Integer integer : getItemRequirements()) {
                MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, integer, 1, false, false);
            }
        }

        return true;
    }

    public boolean enter() {
        if (this.allowParty) {
            if (!(spawns.isEmpty() && getItemRequirements().isEmpty())) {
                if (player.getParty() != null) {
                    if (player.getParty().size() <= getMaxPartySize()) {
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
