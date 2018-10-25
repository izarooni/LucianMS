/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.server.maps;

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.autoban.Cheater;
import com.lucianms.client.autoban.Cheats;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.ServerConstants;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
import com.lucianms.cquest.requirement.CQuestKillRequirement;
import com.lucianms.events.gm.*;
import com.lucianms.features.MonsterPark;
import com.lucianms.features.PlayerBattle;
import com.lucianms.features.carnival.MCarnivalGame;
import com.lucianms.features.carnival.MCarnivalPacket;
import com.lucianms.features.carnival.MCarnivalTeam;
import com.lucianms.features.emergency.Emergency;
import com.lucianms.features.emergency.EmergencyAttack;
import com.lucianms.features.emergency.EmergencyDuel;
import com.lucianms.features.summoning.BlackMageSummoner;
import com.lucianms.features.summoning.ShenronSummoner;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.io.scripting.map.MapScriptManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.*;
import com.lucianms.server.life.*;
import com.lucianms.server.life.MapleLifeFactory.SelfDestruction;
import com.lucianms.server.partyquest.Pyramid;
import com.lucianms.server.world.MaplePartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MapleMap {

    private static final int MAX_CHANCE = 999999;
    private static final Logger LOGGER = LoggerFactory.getLogger(MapleMap.class);
    private static final List<MapleMapObjectType> rangedMapobjectTypes = Arrays.asList(
            MapleMapObjectType.SHOP,
            MapleMapObjectType.ITEM,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.REACTOR
    );

    // this is gonna be weird for a bit
    private ConcurrentMapStorage<Integer, MapleCharacter> characters = new ConcurrentMapStorage<>();
    private ConcurrentMapStorage<Integer, MapleMapObject> mapobjects = new ConcurrentMapStorage<>();
    private Map<Integer, MaplePortal> portals = new HashMap<>();
    private Map<Integer, Integer> backgroundTypes = new HashMap<>();
    private ArrayList<Rectangle> areas = new ArrayList<>();
    private ArrayList<SpawnPoint> spawnPoints = new ArrayList<>();
    private AtomicInteger runningOid = new AtomicInteger(100);
    private AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private MapleFootholdTree footholds = null;
    private String mapName;
    private String streetName;
    private String onUserEnter;
    private String onFirstUserEnter;
    private long timeLimit;
    private int mapid;
    private int decHP = 0;
    private int fieldType;
    private int returnMapId;
    private int channel, world;
    private int fieldLimit = 0;
    private int protectItem = 0;
    private int mobCapacity = -1;
    private int forcedReturnMap = 999999999;
    private short mobInterval = 5000;
    private byte monsterRate;
    private boolean boat;
    private boolean town;
    private boolean clock;
    private boolean docked;
    private boolean dropsOn = true;
    private boolean everlast = false;
    private boolean isOxQuiz = false;
    private boolean summonState = true; // All maps should have this true at the beginning
    private boolean respawnEnabled = true;
    private boolean instanced = false;
    private MapleMapEffect mapEffect = null;
    private MapleOxQuiz ox;
    private Task mapMonitor = null;
    private Pair<Integer, String> timeMob = null;
    private long nextEmergency = 0L;

    //region Henesys PQ
    private int riceCakes = 0;
    private int bunnyDamage = 0;
    //endregion

    //region events
    private boolean eventstarted = false, isMuted = false;
    private MapleSnowball snowball0 = null;
    private MapleSnowball snowball1 = null;
    private MapleCoconut coconut;
    private Point autoKillPosition = null;
    //endregion

    public MapleMap(int mapid, int world, int channel, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.channel = channel;
        this.world = world;
        this.returnMapId = returnMapId;
        this.monsterRate = (byte) Math.ceil(monsterRate);
        if (this.monsterRate == 0) {
            this.monsterRate = 1;
        }
    }

    public int getWorld() {
        return world;
    }

    public int getChannel() {
        return channel;
    }

    public void broadcastMessage(MapleCharacter source, final byte[] packet) {
        getCharacters().stream().filter(c -> c.getId() != source.getId()).forEach(c -> c.getClient().announce(packet));
    }

    public void broadcastGMMessage(MapleCharacter source, final byte[] packet) {
        getCharacters().stream()
                .filter(c -> c.getId() != source.getId() && c.gmLevel() > source.gmLevel())
                .forEach(c -> c.getClient().announce(packet));
    }

    public void toggleDrops() {
        this.dropsOn = !dropsOn;
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> types) {
        return getMapObjects().stream()
                .filter(o -> types.contains(o.getType()) && box.contains(o.getPosition()))
                .collect(Collectors.toList());
    }

    public int getId() {
        return mapid;
    }

    public long getNextEmergency() {
        return nextEmergency;
    }

    public void setNextEmergency(long nextEmergency) {
        this.nextEmergency = nextEmergency;
    }

    public void removeSpawnPoint(int idx) {
        if (idx > 0 && idx < spawnPoints.size()) {
            spawnPoints.remove(idx);
        }
    }

    public MapleMap getReturnMap() {
        return Server.getWorld(world).getChannel(channel).getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public void setReturnMapId(int returnMapId) {
        this.returnMapId = returnMapId;
    }

    public void setReactorState() {
        for (MapleMapObject o : mapobjects.values()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) o).getState() < 1) {
                    ((MapleReactor) o).setState((byte) 1);
                    broadcastMessage(MaplePacketCreator.triggerReactor((MapleReactor) o, 1));
                }
            }
        }
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return Server.getWorld(world).getChannel(channel).getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getTimeLeft() {
        return (int) ((timeLimit - System.currentTimeMillis()) / 1000);
    }

    public int getCurrentPartyId() {
        for (MapleCharacter chr : this.getCharacters()) {
            if (chr.getPartyId() != -1) {
                return chr.getPartyId();
            }
        }
        return -1;
    }

    public void addMapObject(MapleMapObject mapobject) {
        int curOID = getUsableOID();
        mapobject.setObjectId(curOID);
        mapobjects.put(curOID, mapobject);
    }

    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery) {
        spawnAndAddRangedMapObject(mapobject, packetbakery, null);
    }

    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        addMapObject(mapobject);
        for (MapleCharacter chr : getCharacters()) {
            if (!(chr instanceof FakePlayer) && (condition == null || condition.canSpawn(chr))) {
                if (chr.getPosition().distanceSq(mapobject.getPosition()) <= 722500) {
                    packetbakery.sendPackets(chr.getClient());
                    chr.addVisibleMapObject(mapobject);
                }
            }
        }
    }

    private int getUsableOID() {
        if (runningOid.incrementAndGet() > 2000000000) {
            runningOid.set(1000);
        }
        int uoid = runningOid.getAndIncrement();
        if (mapobjects.get(uoid) != null) {
            return getUsableOID();
        }
        return uoid;
    }

    public MapleMapObject removeMapObject(int num) {
        return mapobjects.remove(num);
    }

    public void removeMapObject(final MapleMapObject obj) {
        removeMapObject(obj.getObjectId());
    }

    public Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    public Point calcDropPos(Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 85));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob) {
        if (mob.dropsDisabled() || !dropsOn) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getPosition().x;
        int chServerrate = chr.getDropRate();
        Item idrop;
        byte d = 1;
        Point pos = new Point(0, mob.getPosition().y);

        Map<MonsterStatus, MonsterStatusEffect> stati = mob.getStati();
        if (stati.containsKey(MonsterStatus.SHOWDOWN)) {
            chServerrate *= (stati.get(MonsterStatus.SHOWDOWN).getStati().get(MonsterStatus.SHOWDOWN).doubleValue() / 100.0 + 1.0);
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<MonsterDropEntry> dropEntry = new ArrayList<>(mi.retrieveDrop(mob.getId()));

        // Monster Coin Drop
        if (chr.getGenericEvents().stream().anyMatch(g -> g instanceof MonsterPark)) {
            dropEntry.add(new MonsterDropEntry(4310020, MAX_CHANCE / 100 * 35, 1, 1, (short) -1));
        }

        for (CQuestData qData : chr.getCustomQuests().values()) {
            if (!qData.isCompleted()) {
                for (CQuestItemRequirement.CQuestItem qItem : qData.getToCollect().getItems().values()) {
                    if (qItem.getMonsterId() == mob.getId()) {
                        int chance = (999999 / 1000) * qItem.getChance();
                        dropEntry.add(new MonsterDropEntry(qItem.getItemId(), chance, qItem.getMinQuantity(), qItem.getMaxQuantity(), (short) -1));
                    }
                }
            }
        }

        Collections.shuffle(dropEntry);
        for (MonsterDropEntry de : dropEntry) {
            if (Randomizer.nextInt(MAX_CHANCE) < de.chance * chServerrate) {
                if (droptype == 3) {
                    pos.x = mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2)));
                } else {
                    pos.x = mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                }
                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum;

                    if (mesos > 0) {
                        if (chr.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                            mesos = (int) (mesos * chr.getBuffedValue(MapleBuffStat.MESOUP).doubleValue() / 100.0);
                        }
                        spawnMesoDrop(mesos * chr.getMesoRate(), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
                    }
                } else {
                    if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats(ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid);
                }
                d++;
            }
        }

        // Global Drops
        final List<MonsterGlobalDropEntry> globalEntry = mi.getGlobalDrop();
        for (final MonsterGlobalDropEntry de : globalEntry) {
            int chance = de.chance;
            if (de.itemId >= 4011009 && de.itemId <= 4011009 + 6 && chr.isEyeScannersEquiped()) {
                chance *= 0.10;
            }
            if (Randomizer.nextInt(999999) < chance) {
                if (droptype == 3) {
                    pos.x = mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2)));
                } else {
                    pos.x = mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                }
                if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                    idrop = ii.randomizeStats(ii.getEquipById(de.itemId));
                } else {
                    idrop = new Item(de.itemId, (short) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1));
                }
                spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid);
                d++;
            }
        }
    }

    private void spawnDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final short questid) {
        final MapleMapItem mapItem = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);
        mapItem.setDropTime(System.currentTimeMillis());
        spawnAndAddRangedMapObject(mapItem, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (questid <= 0 || (c.getPlayer().getQuestStatus(questid) == 1 && c.getPlayer().needQuestItem(questid, idrop.getItemId()))) {
                    c.announce(MaplePacketCreator.dropItemFromMapObject(mapItem, mob.getPosition(), dropPos, (byte) 1));
                }
            }
        }, null);

        TaskExecutor.createTask(new ExpireMapItemJob(mapItem), 180000);
        activateItemReactors(chr.getClient(), mapItem);
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);
        mdrop.setDropTime(System.currentTimeMillis());

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.announce(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
            }
        }, null);

        TaskExecutor.createTask(new ExpireMapItemJob(mdrop), 180000);
    }

    public final MapleMapItem disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
        return drop;
    }

    public MapleMonster getMonsterById(int id) {
        for (MapleMapObject obj : getMapObjects()) {
            if (obj.getType() == MapleMapObjectType.MONSTER) {
                if (((MapleMonster) obj).getId() == id) {
                    return (MapleMonster) obj;
                }
            }
        }
        return null;
    }

    public int countMonster(int id) {
        int count = 0;
        for (MapleMapObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
            MapleMonster mob = (MapleMonster) m;
            if (mob.getId() == id) {
                count++;
            }
        }
        return count;
    }

    public boolean damageMonster(final MapleCharacter chr, final MapleMonster monster, final int damage) {
        if (monster.getId() == 8800000) { // zakum
            for (MapleMapObject object : chr.getMap().getMapObjects()) {
                MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        return true;
                    }
                }
            }
        }
        if (monster.isAlive()) {
            boolean killed = false;
            monster.monsterLock.lock();
            try {
                if (!monster.isAlive()) {
                    return false;
                }
                Pair<Integer, Integer> cool = monster.getStats().getCool();

                Pyramid pq = (Pyramid) chr.getPartyQuest();
                if (cool != null && pq != null) {
                    pq.hit(damage, cool);
                    killed = true;
                }

                if (damage > 0) {
                    monster.damage(chr, damage);
                    if (!monster.isAlive()) { // monster just died

                        // Kaneki boss map
                        if (monster.getMap().getId() == 85) {
                            if (chr.getParty() != null) {
                                for (MapleCharacter players : getAllPlayer()) {
                                    players.changeMap(88);
                                    players.dropMessage(5, "Thanks for finally letting me realize my actions were corrupt against the realm.");
                                }
                            } else {
                                chr.changeMap(88);
                                chr.dropMessage(5, "Thanks for finally letting me realize my actions were corrupt against the realm.");
                            }
                        }

                        //region NX Cash gain
                        if (ServerConstants.NX_FROM_MONSTERS) {
                            int random = (Randomizer.nextInt(100)) + 1;
                            int levelDifference = chr.getLevel() - monster.getLevel();
                            if (levelDifference > ServerConstants.MAX_LEVELS_ABOVE) {
                                if (random < ServerConstants.BELOW_LEVERANGEL_NX_CHANCE) {
                                    int receive = monster.getLevel() * 4 / random;
                                    if (chr.getParty() != null) {
                                        receive = (int) (monster.getLevel() * ServerConstants.LEVEL_TO_NX_MULTIPLIER) / chr.getParty().getMembers().size();
                                        for (MaplePartyCharacter players : chr.getParty().getMembers()) {
                                            if (players.isOnline()) {
                                                if (players.getPlayer().getLevel() - chr.getLevel() < -10) {
                                                    players.getPlayer().getCashShop().gainCash(1, receive);
                                                }
                                                if (receive >= 1) {
                                                    players.getPlayer().announce(MaplePacketCreator.earnTitleMessage("You gained " + receive + " NX cash"));
                                                }
                                            }
                                        }
                                    } else if (receive > 0) {
                                        chr.getCashShop().gainCash(1, receive);
                                        chr.announce(MaplePacketCreator.earnTitleMessage("You gained " + receive + " NX cash"));
                                    }
                                }
                            } else if (levelDifference > -ServerConstants.MAX_LEVELS_BELOW) {
                                if (random < ServerConstants.BELOW_LEVERANGEL_NX_CHANCE) {
                                    int receive = (int) (monster.getLevel() * ServerConstants.LEVEL_TO_NX_MULTIPLIER + random);
                                    if (chr.getParty() != null) {
                                        receive = (int) (monster.getLevel() * ServerConstants.LEVEL_TO_NX_MULTIPLIER + random - (random / 2)) / chr.getParty().getMembers().size();
                                        for (MaplePartyCharacter players : chr.getParty().getMembers()) {
                                            if (players.isOnline()) {
                                                if (players.getPlayer().getLevel() - chr.getLevel() < -10) {
                                                    players.getPlayer().getCashShop().gainCash(1, receive);
                                                }
                                                if (receive >= 1) {
                                                    players.getPlayer().announce(MaplePacketCreator.earnTitleMessage("You gained " + receive + " NX cash"));
                                                }
                                            }
                                        }
                                    } else if (receive > 0) {
                                        chr.getCashShop().gainCash(1, receive);
                                        if (receive >= 1) {
                                            chr.announce(MaplePacketCreator.earnTitleMessage("You gained " + receive + " NX cash"));
                                        }
                                    }

                                }

                            }
                        }
                        //endregion

                        if (chr.getArcade() != null) {
                            chr.getArcade().onKill(monster.getId());
                            if (monster.getId() == 9500365) {
                                MapleMonster mob = MapleLifeFactory.getMonster(2230103);
                                mob.setHp(2147000000);
                                mob.setMp(2147000000);
                                mob.setLevel(Integer.MAX_VALUE);
                                spawnMonsterOnGroudBelow(mob, monster.getPosition());
                            }
                        }
                        if (monster.getId() == chr.getKillType() && chr.getCurrent() < chr.getGoal()) {
                            chr.setCurrent(chr.getCurrent() + 1);
                            if (!(chr.getCurrent() > chr.getGoal())) {
                                chr.dropMessage(6, "You have killed " + chr.getCurrent() + " out of " + chr.getGoal() + " " + monster.getName() + "'s");
                            }
                        }
                        killed = true;
                    }
                } else if (monster.getId() >= 8810002 && monster.getId() <= 8810009) { // horntail
                    for (MapleMapObject object : getMapObjects()) {
                        MapleMonster mons = getMonsterByOid(object.getObjectId());
                        if (mons != null) {
                            if (monster.isAlive() && (monster.getId() >= 8810010 && monster.getId() <= 8810017)) {
                                if (mons.getId() == 8810018) {
                                    killMonster(mons, chr, true);
                                }
                            }
                        }
                    }
                }
            } finally {
                monster.monsterLock.unlock();
            }
            if (monster.getStats().getSelfDestruction() != null && monster.getStats().getSelfDestruction().getHp() > -1) {// should
                if (monster.getHp() <= monster.getStats().getSelfDestruction().getHp()) {
                    killMonster(monster, chr, true, false, monster.getStats().getSelfDestruction().getAction());
                    return true;
                }
            }
            if (!monster.isAlive() || killed) {
                killMonster(monster, chr, true);
            }
            return true;
        }
        return false;
    }

    public List<MapleMonster> getMonsters() {
        return getMapObjects().stream().filter(o -> (o instanceof MapleMonster)).map(o -> (MapleMonster) o).collect(Collectors.toList());
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops) {
        killMonster(monster, chr, withDrops, false, 1);
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean secondTime, int animation) {
        if (monster.getId() == 8810018 && !secondTime) {
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    killMonster(monster, chr, withDrops, true, 1);
                    killAllMonsters();
                }
            }, 3000);
            return;
        }
        if (chr == null) {
            spawnedMonstersOnMap.decrementAndGet();
            monster.killBy(chr);
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
            removeMapObject(monster);
            return;
        } else {
            for (CQuestData data : chr.getCustomQuests().values()) {
                if (!data.isCompleted()) {
                    CQuestKillRequirement toKill = data.getToKill();
                    Pair<Integer, Integer> p = toKill.get(monster.getId());
                    if (p != null && p.right < p.left) { // don't exceed requirement variable
                        toKill.incrementRequirement(monster.getId(), 1); // increment progress
                        chr.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Monster killed '%s' [%d / %d]", data.getName(), monster.getName(), p.right, p.left)));
                        boolean checked = toKill.isFinished(); // store to local variable before updating
                        if (data.checkRequirements() && !checked) { // update checked; if requirement is finished and previously was not...
                            chr.announce(MaplePacketCreator.getShowQuestCompletion(1));
                            chr.announce(MaplePacketCreator.earnTitleMessage(String.format("Quest '%s' completed!", data.getName())));
                            chr.announce(MaplePacketCreator.serverNotice(5, String.format("Quest '%s' completed!", data.getName())));

                        }
                    }
                }
            }
            Equip weapon = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (weapon != null) {
                weapon.setEliminations(weapon.getEliminations() + 1);
            }
        }
        if (monster.getStats().getLevel() >= chr.getLevel() + 30 && !chr.isGM()) {
            Cheater.CheatEntry entry = chr.getCheater().getCheatEntry(Cheats.UnderLevelAttack);
            entry.incrementCheatCount();
            entry.announce(chr.getClient(), 8000, "[{}] {} (level {}) attacked a monster ({}) too high of level (level {})", entry.cheatCount, chr.getName(), chr.getLevel(), monster.getId(), monster.getStats().getLevel());
        }
        int buff = monster.getBuffToGive();
        if (buff > -1) {
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            for (MapleMapObject mmo : getAllPlayer()) {
                MapleCharacter character = (MapleCharacter) mmo;
                if (character.isAlive()) {
                    MapleStatEffect statEffect = mii.getItemEffect(buff);
                    character.getClient().announce(MaplePacketCreator.showOwnBuffEffect(buff, 1));
                    broadcastMessage(character, MaplePacketCreator.showBuffeffect(character.getId(), buff, 1), false);
                    statEffect.applyTo(character);
                }
            }
        }
        if (monster.getId() == 8810018 && chr.getMapId() == 240060200) {
            chr.getClient().getWorldServer().broadcastMessage(6, "To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
        }
        if (monster.getId() == 9895253 && getId() == 97) { // Black Mage
            List<MapleCharacter> warp = new ArrayList<>(getCharacters());
            TaskExecutor.createTask(() -> warp.forEach(c -> c.changeMap(333)), 2500);
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), animation));
        // if (monster.getStats().selfDestruction() == null) {//FUU BOMBS D:
        removeMapObject(monster);
        // }
        MCarnivalGame carnivalGame = (MCarnivalGame) chr.getGenericEvents().stream().filter(o -> o instanceof MCarnivalGame).findFirst().orElse(null);
        if (monster.getCP() > 0 && carnivalGame != null) {
            carnivalGame.getTeam(chr.getTeam()).addCarnivalPoints(chr, monster.getCP());
            chr.announce(MCarnivalPacket.getMonsterCarnivalPointsUpdate(chr.getCP(), chr.getObtainedCP()));
            broadcastMessage(MCarnivalPacket.getMonsterCarnivalPointsUpdateParty(carnivalGame.getTeam(chr.getTeam())));
            // they drop items too ):
        }
        if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMapObject> objects = getMapObjects();
            for (MapleMapObject object : objects) {
                MapleMonster mons = getMonsterByOid(object.getObjectId());
                if (mons != null) {
                    if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                        makeZakReal = false;
                        break;
                    }
                }
            }
            if (makeZakReal) {
                for (MapleMapObject object : objects) {
                    MapleMonster mons = chr.getMap().getMonsterByOid(object.getObjectId());
                    if (mons != null) {
                        if (mons.getId() == 8800000) {
                            makeMonsterReal(mons);
                            updateMonsterController(mons);
                            break;
                        }
                    }
                }
            }
        }
        MapleCharacter dropOwner = monster.killBy(chr);
        if (withDrops && !monster.dropsDisabled()) {
            if (dropOwner == null) {
                dropOwner = chr;
            }
            dropFromMonster(dropOwner, monster);
        }
    }

    public void killFriendlies(MapleMonster mob) {
        this.killMonster(mob, getAllPlayer().get(0), false);
    }

    public void killMonster(int monsId) {
        for (MapleMapObject mmo : getMapObjects()) {
            if (mmo instanceof MapleMonster) {
                if (((MapleMonster) mmo).getId() == monsId) {
                    this.killMonster((MapleMonster) mmo, getAllPlayer().get(0), false);
                }
            }
        }
    }

    public void monsterCloakingDevice() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            broadcastMessage(MaplePacketCreator.makeMonsterInvisible(monster));
        }
    }

    public void softKillAllMonsters() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getStats().isFriendly()) {
                continue;
            }
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            removeMapObject(monster);
        }
    }

    public void killAllMonstersNotFriendly() {
        for (MapleMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.MONSTER))) {
            MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getStats().isFriendly()) {
                continue;
            }
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
        }
    }

    public void killAllMonsters() {
        for (MapleMonster monster : getMapObjects(MapleMonster.class)) {
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            monster.killBy(null);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
            removeMapObject(monster);
        }
    }

    public List<MapleCharacter> getAllPlayer() {
        return getMapObjects(MapleCharacter.class);
    }

    public void destroyReactor(int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor.isAlive()) {
            reactor.setAlive(false);
            reactor.setTimerActive(false);
            broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
            removeMapObject(reactor);
            if (reactor.getDelay() > 0) {
                TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        respawnReactor(reactor);
                    }
                }, reactor.getDelay());
            }
        }
    }

    public void resetReactors() {
        for (MapleMapObject o : getMapObjects()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                final MapleReactor r = ((MapleReactor) o);
                r.setState((byte) 0);
                r.setTimerActive(false);
                broadcastMessage(MaplePacketCreator.triggerReactor(r, 0));
            }
        }
    }

    public void shuffleReactors() {
        List<Point> points = new ArrayList<>();
        for (MapleMapObject o : getMapObjects()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                points.add(o.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject o : getMapObjects()) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                o.setPosition(points.remove(points.size() - 1));
            }
        }
        points.clear();
    }

    public MapleReactor getReactorById(int Id) {
        for (MapleMapObject obj : getMapObjects()) {
            if (obj.getType() == MapleMapObjectType.REACTOR) {
                if (((MapleReactor) obj).getId() == Id) {
                    return (MapleReactor) obj;
                }
            }
        }
        return null;
    }

    /**
     * Automagically finds a new controller for the given monster from the chars on the map...
     *
     * @param monster
     */
    public void updateMonsterController(MapleMonster monster) {
        monster.monsterLock.lock();
        try {
            if (!monster.isAlive()) {
                return;
            }
            if (monster.getController() != null) {
                if (monster.getController().getMap() != this) {
                    monster.getController().stopControllingMonster(monster);
                } else {
                    return;
                }
            }
            int mincontrolled = -1;
            MapleCharacter newController = null;
            for (MapleCharacter chr : getCharacters()) {
                if (!(chr instanceof FakePlayer) && !chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
                    mincontrolled = chr.getControlledMonsters().size();
                    newController = chr;
                }
            }
            if (newController != null) {// was a new controller found? (if not
                // no one is on the map)
                if (monster.isFirstAttack()) {
                    newController.controlMonster(monster, true);
                    monster.setControllerHasAggro(true);
                    monster.setControllerKnowsAboutAggro(true);
                } else {
                    newController.controlMonster(monster, false);
                }
            }
        } finally {
            monster.monsterLock.unlock();
        }
    }

    public Collection<MapleMapObject> getMapObjects() {
        return mapobjects.values();
    }

    public ArrayList<MapleCharacter> getCharacters() {
        return new ArrayList<>(characters.values());
    }

    public boolean containsNPC(int npcid) {
        if (npcid == 9000066) {
            return true;
        }
        for (MapleMapObject obj : getMapObjects()) {
            if (obj.getType() == MapleMapObjectType.NPC) {
                if (((MapleNPC) obj).getId() == npcid) {
                    return true;
                }
            }
        }
        return false;
    }

    public MapleMapObject getMapObject(int oid) {
        return mapobjects.get(oid);
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns null
     *
     * @param oid
     * @return
     */
    public MapleMonster getMonsterByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        if (mmo.getType() == MapleMapObjectType.MONSTER) {
            return (MapleMonster) mmo;
        }
        return null;
    }

    public MapleReactor getReactorByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid);
        if (mmo == null) {
            return null;
        }
        return mmo.getType() == MapleMapObjectType.REACTOR ? (MapleReactor) mmo : null;
    }

    public MapleReactor getReactorByName(String name) {
        for (MapleReactor reactor : getReactors()) {
            if (reactor.getName().equalsIgnoreCase(name)) {
                return reactor;
            }
        }
        return null;
    }

    public void addCarnivalMonster(MapleMonster monster, MCarnivalTeam team) {
        if (spawnPoints.isEmpty()) {
            LOGGER.warn("Cannot summon Monster Carnival mob due to empty spawn points");
            return;
        }
        SpawnPoint selected = spawnPoints.stream().filter(sp -> sp.getTeam() == team.getId()).findAny().orElse(null);
        if (selected == null) {
            LOGGER.warn("Cannot summon Monster Carnival mob because there are no matching spawn points");
            return;
        }
        Point nPosition = selected.getPosition();
        monster.setPosition(nPosition);
        SpawnPoint spawnPoint = new SpawnPoint(this, monster, false, 1, team.getId());
        addMonsterSpawnPoint(spawnPoint);

        spawnPoint.getMonster();
        spawnPoint.summonMonster();
    }

    public MapleMonster spawnMonsterOnGroudBelow(int id, int x, int y) {
        MapleMonster mob = MapleLifeFactory.getMonster(id);
        spawnMonsterOnGroundBelow(mob, new Point(x, y));
        return mob;
    }

    public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        if (spos != null) {
            spos.y--;
        } else {
            spos = mob.getPosition().getLocation();
        }
        mob.setPosition(spos);
        spawnMonster(mob);
    }

    public void addBunnyHit() {
        bunnyDamage++;
        if (bunnyDamage > 5) {
            broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny is feeling sick. Please protect it so it can make delicious rice cakes."));
            bunnyDamage = 0;
        }
    }

    private void monsterItemDrop(final MapleMonster m, final Item item, long delay) {
        final Task task = TaskExecutor.createRepeatingTask(new Runnable() {
            @Override
            public void run() {
                if (MapleMap.this.getMonsterById(m.getId()) != null && !MapleMap.this.getAllPlayer().isEmpty()) {
                    if (item.getItemId() == 4001101) {
                        MapleMap.this.riceCakes++;
                        MapleMap.this.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny made rice cake number " + (MapleMap.this.riceCakes)));
                    }
                    spawnItemDrop(m, getAllPlayer().get(0), item, m.getPosition(), false, false);
                }
            }
        }, delay, delay);
        if (getMonsterById(m.getId()) == null) {
            task.cancel();
        }
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = getGroundBelow(pos);
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public Point getGroundBelow(Point pos) {
        Point spos = new Point(pos.x, pos.y - 3); // Using -3 fixes issues with spawning pets causing a lot of issues.
        spos = calcPointBelow(spos);
        if (spos == null) {
            LOGGER.info("Unable to find position below {} in map {}", pos.toString(), getId());
            return null;
        }
        spos.y--;
        return spos;
    }

    public void spawnRevives(final MapleMonster monster) {
        monster.setMap(this);

        spawnAndAddRangedMapObject(monster, c -> c.announce(MaplePacketCreator.spawnMonster(monster, false)));
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonster(final MapleMonster monster) {
        if (mobCapacity != -1 && mobCapacity <= spawnedMonstersOnMap.get()) {
            LOGGER.info("Unable to spawn monster due to mob capacity {} in map {}", mobCapacity, getId());
            return;
        }
        monster.setMap(this);
        if (!monster.getMap().getAllPlayer().isEmpty()) {
            MapleCharacter chr = getAllPlayer().get(0);
            if (monster.getEventInstance() == null && chr.getEventInstance() != null) {
                chr.getEventInstance().registerMonster(monster);
            }
        }

        spawnAndAddRangedMapObject(monster, c -> c.announce(MaplePacketCreator.spawnMonster(monster, true)), null);
        updateMonsterController(monster);

        if (monster.getDropPeriodTime() > 0) { // 9300102 - Watchhog, 9300061 -
            // Moon Bunny (HPQ)
            if (monster.getId() == 9300102) {
                monsterItemDrop(monster, new Item(4031507, (short) 0, (short) 1), monster.getDropPeriodTime());
            } else if (monster.getId() == 9300061) {
                monsterItemDrop(monster, new Item(4001101, (short) 0, (short) 1), monster.getDropPeriodTime() / 3);
            } else {
                LOGGER.warn("Unhandled timed monster", monster.getId());
            }
        }
        spawnedMonstersOnMap.incrementAndGet();
        final SelfDestruction selfDestruction = monster.getStats().getSelfDestruction();
        if (monster.getStats().getRemoveAfter() > 0 || selfDestruction != null && selfDestruction.getHp() < 0) {
            if (selfDestruction == null) {
                TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        killMonster(monster, null, false);
                    }
                }, monster.getStats().getRemoveAfter());
            } else {
                TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        killMonster(monster, null, false, false, selfDestruction.getAction());
                    }
                }, selfDestruction.getRemoveAfter());
            }
        }
    }

    public void spawnDojoMonster(final MapleMonster monster) {
        Point[] pts = {new Point(140, 0), new Point(190, 7), new Point(187, 7)};
        spawnMonsterWithEffect(monster, 15, pts[Randomizer.nextInt(3)]);
    }

    public void spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        monster.setMap(this);
        Point spos = new Point(pos.x, pos.y - 1);
        spos = calcPointBelow(spos);
        if (spos == null) {
            LOGGER.warn("Unable to spawn monster {} with effect {} in map {} at position {}", monster.getId(), effect, getId(), pos.toString());
            return;
        }
        spos.y--;
        monster.setPosition(spos);
        if (mapid < 925020000 || mapid > 925030000) {
            monster.disableDrops();
        }
        spawnAndAddRangedMapObject(monster, c -> c.announce(MaplePacketCreator.spawnMonster(monster, true, effect)));
        if (monster.hasBossHPBar()) {
            broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
        }
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        spawnAndAddRangedMapObject(monster, c -> c.announce(MaplePacketCreator.spawnFakeMonster(monster, 0)));
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void makeMonsterReal(final MapleMonster monster) {
        monster.setFake(false);
        broadcastMessage(MaplePacketCreator.makeMonsterReal(monster));
        updateMonsterController(monster);
    }

    public void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, c -> c.announce(reactor.makeSpawnData()));
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.announce(MaplePacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(c.getPlayer().getMPC()))) {
                    c.announce(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.announce(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.announce(MaplePacketCreator.enableActions());
            }
        }, chr -> chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null);

    }

    public List<MapleCharacter> getPlayersInRange(Rectangle box, List<MapleCharacter> chr) {
        List<MapleCharacter> inRange = new LinkedList<>();
        for (MapleCharacter a : getCharacters()) {
            if (chr.contains(a)) {
                if (box.contains(a.getPosition())) {
                    inRange.add(a);
                }
            }
        }
        return inRange;
    }

    public void spawnSummon(final MapleSummon summon) {
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (summon != null) {
                    c.announce(MaplePacketCreator.spawnSummon(summon, true));
                }
            }
        }, null);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean poison, boolean fake, boolean recovery) {
        addMapObject(mist);
        broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
        final Task poisonTask;
        if (poison) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    List<MapleMapObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER));
                    for (MapleMapObject mo : affectedMonsters) {
                        if (mist.makeChanceResult()) {
                            MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), null, false);
                            ((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
                        }
                    }
                }
            };
            poisonTask = TaskExecutor.createRepeatingTask(run, 2000, 2500);
        } else if (recovery) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    List<MapleMapObject> players = getMapObjectsInBox(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER));
                    for (MapleMapObject mo : players) {
                        if (mist.makeChanceResult()) {
                            MapleCharacter chr = (MapleCharacter) mo;
                            if (mist.getOwner().getId() == chr.getId() || mist.getOwner().getParty() != null && mist.getOwner().getParty().containsMembers(chr.getMPC())) {
                                chr.addMP(mist.getSourceSkill().getEffect(chr.getSkillLevel(mist.getSourceSkill().getId())).getX() * chr.getMp() / 100);
                            }
                        }
                    }
                }
            };
            poisonTask = TaskExecutor.createRepeatingTask(run, 2000, 2500);
        } else {
            poisonTask = null;
        }
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                removeMapObject(mist);
                broadcastGMMessage(mist.makeDestroyData());
                if (poisonTask != null) {
                    poisonTask.cancel();
                }
            }
        }, duration);
    }

    public MapleMapItem spawnItemDrop(MapleCharacter owner, MapleMapObject dropper, int itemId, short quantity, Point position, boolean disappear) {
        final Point dropPosition = calcDropPos(position, position);

        Item item = new Item(itemId, quantity, (short) -1);
        MapleMapItem mapItem = new MapleMapItem(item, dropPosition, dropper, owner, (byte) 2, false);
        mapItem.setDropTime(System.currentTimeMillis());

        spawnAndAddRangedMapObject(mapItem, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.announce(MaplePacketCreator.dropItemFromMapObject(mapItem, dropper.getPosition(), dropPosition, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mapItem, dropper.getPosition(), dropPosition, (byte) 0));
        if (disappear) {
            TaskExecutor.createTask(new ExpireMapItemJob(mapItem), 180000);
            activateItemReactors(owner.getClient(), mapItem);
        }
        return mapItem;
    }

    public final MapleMapItem spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) (ffaDrop ? 2 : 0), playerDrop);
        drop.setDropTime(System.currentTimeMillis());

        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.announce(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1));
            }
        }, null);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));

        if (!everlast) {
            TaskExecutor.createTask(new ExpireMapItemJob(drop), 180000);
            activateItemReactors(owner.getClient(), drop);
        }
        return drop;
    }

    private void activateItemReactors(MapleClient client, MapleMapItem mapItem) {
        Item item = mapItem.getItem();
        for (MapleReactor reactor : getReactors()) {
            if (reactor.getReactorType() == 100) {
                Pair<Integer, Integer> pair = reactor.getReactItem((byte) 0);
                if (pair.getLeft() == item.getItemId() && pair.getRight() == item.getQuantity()) {
                    if (reactor.getArea().contains(mapItem.getPosition()) && !reactor.isTimerActive()) {
                        TaskExecutor.createTask(new ActivateItemReactor(client, mapItem, reactor), 5000);
                        reactor.setTimerActive(true);
                        break;
                    }
                }
            }
        }
    }

    public final List<MapleReactor> getReactors() {
        return getMapObjects().stream()
                .filter(o -> o instanceof MapleReactor)
                .map(o -> (MapleReactor) o)
                .collect(Collectors.toList());
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, long time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        broadcastMessage(mapEffect.makeStartData());
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, time);
    }

    public void addFakePlayer(FakePlayer fakePlayer) {
        mapobjects.put(fakePlayer.getObjectId(), fakePlayer);
        characters.put(fakePlayer.getObjectId(), fakePlayer);
        broadcastMessage(fakePlayer, MaplePacketCreator.spawnPlayerMapobject(fakePlayer), false);
    }

    public void removeFakePlayer(FakePlayer fakePlayer) {
        mapobjects.remove(fakePlayer.getObjectId());
        characters.remove(fakePlayer.getObjectId());
        broadcastMessage(fakePlayer, MaplePacketCreator.removePlayerFromMap(fakePlayer.getId()), false);
    }

    public void addPlayer(final MapleCharacter chr) {
        characters.put(chr.getObjectId(), chr);

        chr.setMapId(mapid);
        if (onFirstUserEnter.length() != 0 && !chr.hasEntered(onFirstUserEnter, mapid) && MapScriptManager.getInstance().scriptExists(onFirstUserEnter, true)) {
            if (getAllPlayer().size() <= 1) {
                chr.enteredScript(onFirstUserEnter, mapid);
                MapScriptManager.getInstance().getMapScript(chr.getClient(), onFirstUserEnter, true);
            }
        }
        if (onUserEnter != null && !onUserEnter.isEmpty()) {
            if (onUserEnter.equals("cygnusTest") && (mapid < 913040000 || mapid > 913040006)) {
                chr.saveLocation("INTRO");
            }
            MapScriptManager.getInstance().getMapScript(chr.getClient(), onUserEnter, false);
        }
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            chr.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
        }
        if (mapid == 923010000 && getMonsterById(9300102) == null) { // Kenta's Mount quest
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300102), new Point(77, 426));
        }

        //region map timeouts
        if (mapid == 910010200) { // Henesys Party Quest Bonus
            chr.announce(MaplePacketCreator.getClock(60 * 5));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 910010200) {
                        chr.changeMap(910010400);
                    }
                }
            }, 5 * 60 * 1000);
        } else if (mapid == 200090060) { // To Rien
            chr.announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090060) {
                        chr.changeMap(140020300);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090070) { // To Lith Harbor
            chr.announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090070) {
                        chr.changeMap(104000000, 3);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090030) { // To Ereve (SkyFerry)
            chr.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090030) {
                        chr.changeMap(130000210);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090031) { // To Victoria Island (SkyFerry)
            chr.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090031) {
                        chr.changeMap(101000400);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090021) { // To Orbis (SkyFerry)
            chr.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090021) {
                        chr.changeMap(200000161);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090020) { // To Ereve From Orbis (SkyFerry)
            chr.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (chr.getMapId() == 200090020) {
                        chr.changeMap(130000210);
                    }
                }
            }, 60 * 1000);
        }
        //endregion

        EventInstanceManager eim = chr.getEventInstance();

        if (mapid == 103040400) {
            if (eim != null) {
                eim.movePlayer(chr, this);
            }
        } else if (MapleMiniDungeon.isDungeonMap(mapid)) {
            final MapleMiniDungeon dungeon = MapleMiniDungeon.getDungeon(mapid);
            chr.getClient().announce(MaplePacketCreator.getClock(30 * 60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (MapleMiniDungeon.isDungeonMap(chr.getMapId())) {
                        chr.changeMap(dungeon.getBase());
                    }
                }
            }, 30 * 60 * 1000);
        } else if (mapid == 97) {
            new BlackMageSummoner().registerPlayer(chr);
        } else if (mapid == 908) {
            new ShenronSummoner().registerPlayer(chr);
        }
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < chr.getPets().length; i++) {
            if (pets[i] != null) {
                Point nPosition = getGroundBelow(chr.getPosition());
                if (nPosition == null) {
                    nPosition = chr.getPosition().getLocation();
                }
                pets[i].setPos(nPosition);
                chr.announce(MaplePacketCreator.showPet(chr, pets[i], false, false));
            } else {
                break;
            }
        }
        if (chr.isHidden()) {
            broadcastGMMessage(chr, MaplePacketCreator.spawnPlayerMapobject(chr), false);
            chr.announce(MaplePacketCreator.getGMEffect(0x10, (byte) 1));

            List<Pair<MapleBuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(MapleBuffStat.DARKSIGHT, 0));
            broadcastGMMessage(chr, MaplePacketCreator.giveForeignBuff(chr.getId(), dsstat), false);
        } else {
            broadcastMessage(chr, MaplePacketCreator.spawnPlayerMapobject(chr), false);
        }

        sendObjectPlacement(chr.getClient());
        mapobjects.put(chr.getObjectId(), chr);

        if (isStartingEventMap() && !eventStarted()) {
            chr.getMap().getPortal("join00").setPortalStatus(false);
        }
        if (hasForcedEquip()) {
            chr.getClient().announce(MaplePacketCreator.showForcedEquip(-1));
        }
        if (specialEquip()) {
            chr.getClient().announce(MaplePacketCreator.coconutScore(0, 0));
            chr.getClient().announce(MaplePacketCreator.showForcedEquip(chr.getTeam()));
        }
        if (chr.getPlayerShop() != null) {
            addMapObject(chr.getPlayerShop());
        }

        final MapleDragon dragon = chr.getDragon();
        if (dragon != null) {
            dragon.setPosition(chr.getPosition());
            addMapObject(dragon);
            if (chr.isHidden()) {
                broadcastGMMessage(chr, MaplePacketCreator.spawnDragon(dragon));
            } else {
                broadcastMessage(chr, MaplePacketCreator.spawnDragon(dragon));
            }
        } else if (GameConstants.hasSPTable(chr.getJob())) {
            chr.createDragon();
            broadcastMessage(MaplePacketCreator.spawnDragon(chr.getDragon()));
        }

        MapleStatEffect summonStat = chr.getStatForBuff(MapleBuffStat.SUMMON);
        if (summonStat != null) {
            MapleSummon summon = chr.getSummons().get(summonStat.getSourceId());
            summon.setPosition(chr.getPosition());
            chr.getMap().spawnSummon(summon);
            updateMapObjectVisibility(chr, summon);
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        chr.getClient().announce(MaplePacketCreator.resetForcedStats());
        if (mapid == 914000200 || mapid == 914000210 || mapid == 914000220) {
            chr.getClient().announce(MaplePacketCreator.aranGodlyStats());
        }
        if (eim != null && eim.isTimerStarted()) {
            chr.getClient().announce(MaplePacketCreator.getClock((int) (eim.getTimeLeft() / 1000)));
        }
        if (chr.getFitness() != null && chr.getFitness().isTimerStarted()) {
            chr.getClient().announce(MaplePacketCreator.getClock((int) (chr.getFitness().getTimeLeft() / 1000)));
        }

        if (chr.getOla() != null && chr.getOla().isTimerStarted()) {
            chr.getClient().announce(MaplePacketCreator.getClock((int) (chr.getOla().getTimeLeft() / 1000)));
        }

        if (mapid == 109060000) {
            chr.announce(MaplePacketCreator.rollSnowBall(true, 0, null, null));
        }

        //region PvP enabled maps
        {
            PlayerBattle battle = chr.getPlayerBattle();
            if (Arrays.binarySearch(
                    Server.getConfig().getIntArray("PvPEnabled"), getId()) > -1) {
                if (battle == null) {
                    new PlayerBattle().registerPlayer(chr);
                    chr.sendMessage(6, "You have entered a PvP map");
                }
            } else if (battle != null) {
                if (battle.getLastAttack() < 5) {
                    chr.sendMessage(6, "You remain in combat mode due to attacking or being attacked too recently");
                } else {
                    battle.unregisterPlayer(chr);
                    chr.sendMessage(6, "You are no longer in PvP mode");
                }
            }
        }
        //endregion

        //region emergency attack
        // empty map or contains only party members
        if (characters.size() == 1
                || (chr.getPartyId() > 0 && getCharacters().stream().allMatch(p -> p.getPartyId() == chr.getPartyId()))) {
            /*
            May only activate under the following conditions:
            Contains monster spawn points,
            Contains no boss-type monsters,
            Map is a non-town type,
            Is not explicity excluded via server configuration
             */
            // must be a map that contains monster spawnpoints, contains no boss entity and is a hutning field
            if (!isTown()
                    && spawnPoints.stream().noneMatch(sp -> sp.getMonster().isBoss() || chr.getLevel() - sp.getMonster().getLevel() > 30)
                    && !spawnPoints.isEmpty()
                    && Arrays.binarySearch(Server.getConfig().getIntArray("EmergencyExcludes"), getId()) < 0) {
                // 1/25 chance to trigger emergency
                if ((chr.isGM() && chr.isDebug())
                        || ((System.currentTimeMillis() > nextEmergency)
                        && Randomizer.nextInt(25) == 0
                        && chr.getGenericEvents().isEmpty()
                        && eim == null
                        && chr.getArcade() == null)) {
                    Emergency event = Randomizer.nextBoolean() && chr.getLevel() >= 30 ? new EmergencyDuel(chr) : new EmergencyAttack(chr);
                    TaskExecutor.createTask(new Runnable() {
                        @Override
                        public void run() {
                            event.registerPlayer(chr);
                            if (!event.isCanceled()) {
                                nextEmergency = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(8);
                            }
                        }
                    }, 1500);
                } else {
                    nextEmergency = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
                }
            }
        }
        //endregion

        MCarnivalGame carnivalGame = (MCarnivalGame) chr.getGenericEvents().stream().filter(o -> o instanceof MCarnivalGame).findFirst().orElse(null);
        if (carnivalGame != null && (mapid == 980000101 || mapid == 980000201 || mapid == 980000301 || mapid == 980000401 || mapid == 980000501 || mapid == 980000601)) {
            chr.announce(MaplePacketCreator.getClock((int) (carnivalGame.getTimeLeft() / 1000)));
            chr.announce(MCarnivalPacket.getMonsterCarnivalStart(chr, carnivalGame));
            chr.announce(MaplePacketCreator.showForcedEquip(chr.getTeam()));
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.getClient().announce((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (hasBoat() == 2) {
            chr.getClient().announce((MaplePacketCreator.boatPacket(true)));
        } else if (hasBoat() == 1 && (chr.getMapId() != 200090000 || chr.getMapId() != 200090010)) {
            chr.getClient().announce(MaplePacketCreator.boatPacket(false));
        }
        chr.receivePartyMemberHP();
        if (chr.getDragon() == null && GameConstants.hasSPTable(chr.getJob())) {
            chr.createDragon();
            broadcastMessage(MaplePacketCreator.spawnDragon(chr.getDragon()));
        }
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal getRandomSpawnpoint() {
        List<MaplePortal> spawnPoints = new ArrayList<>();
        for (MaplePortal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints.add(portal);
            }
        }
        MaplePortal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        return portal != null ? portal : getPortal(0);
    }

    public void removePlayer(MapleCharacter chr) {
        characters.remove(chr.getObjectId());
        removeMapObject(chr.getObjectId());
        if (!chr.isHidden()) {
            broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        } else {
            broadcastGMMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        }

        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.leaveMap();
        for (MapleSummon summon : chr.getSummons().values()) {
            if (summon.isStationary()) {
                chr.cancelBuffStats(MapleBuffStat.PUPPET);
            } else {
                removeMapObject(summon);
            }
        }
        if (chr.getDragon() != null) {
            removeMapObject(chr.getDragon());
            chr.setDragon(null);
            if (chr.isHidden()) {
                broadcastGMMessage(chr, MaplePacketCreator.removeDragon(chr.getId()));
            } else {
                broadcastMessage(chr, MaplePacketCreator.removeDragon(chr.getId()));
            }
        }
    }

    public void broadcastMessage(final byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastGMMessage(final byte[] packet) {
        broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(MapleCharacter source, final byte[] packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(MapleCharacter source, final byte[] packet, boolean repeatToSource, boolean ranged) {
        broadcastMessage(repeatToSource ? null : source, packet, ranged ? 722500 : Double.POSITIVE_INFINITY, source.getPosition());
    }

    public void broadcastMessage(final byte[] packet, Point rangedFrom) {
        broadcastMessage(null, packet, 722500, rangedFrom);
    }

    public void broadcastMessage(MapleCharacter source, final byte[] packet, Point rangedFrom) {
        broadcastMessage(source, packet, 722500, rangedFrom);
    }

    private void broadcastMessage(MapleCharacter source, final byte[] packet, double rangeSq, Point rangedFrom) {
        for (MapleCharacter chr : getCharacters()) {
            if (chr != source && !(chr instanceof FakePlayer)) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.getClient().announce(packet);
                    }
                } else {
                    chr.getClient().announce(packet);
                }
            }
        }
    }

    private boolean isNonRangedType(MapleMapObjectType type) {
        switch (type) {
            case NPC:
            case PLAYER:
            case HIRED_MERCHANT:
            case PLAYER_NPC:
            case DRAGON:
            case MIST:
                return true;
            default:
                return false;
        }
    }

    private void sendObjectPlacement(MapleClient mapleClient) {
        MapleCharacter chr = mapleClient.getPlayer();

        for (MapleMapObject o : getMapObjects()) {
            if (o.getObjectId() == chr.getObjectId()) {
                continue;
            }
            if (o.getType() == MapleMapObjectType.SUMMON) {
                MapleSummon summon = (MapleSummon) o;
                if (summon.getOwner() == chr) {
                    if (chr.getSummons().isEmpty() || !chr.getSummons().containsValue(summon)) {
                        mapobjects.remove(o.getObjectId());
                        continue;
                    }
                }
            }
            if (isNonRangedType(o.getType())) {
                o.sendSpawnData(mapleClient);
            } else if (o.getType() == MapleMapObjectType.MONSTER) {
                updateMonsterController((MapleMonster) o);
            }
        }
        if (chr != null) {
            for (MapleMapObject o : getMapObjectsInRange(chr.getPosition(), 722500, rangedMapobjectTypes)) {
                if (o.getType() == MapleMapObjectType.REACTOR) {
                    if (((MapleReactor) o).isAlive()) {
                        o.sendSpawnData(chr.getClient());
                        chr.addVisibleMapObject(o);
                    }
                } else {
                    o.sendSpawnData(chr.getClient());
                    chr.addVisibleMapObject(o);
                }
            }
        }
    }

    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<>();
        for (MapleMapObject l : getMapObjects()) {
            if (types.contains(l.getType())) {
                if (from.distanceSq(l.getPosition()) <= rangeSq) {
                    ret.add(l);
                }
            }
        }
        return ret;
    }

    public <T> ArrayList<T> getMapObjects(Class<T> t) {
        if (!MapleMapObject.class.isAssignableFrom(t)) {
            throw new IllegalArgumentException();
        }
        ArrayList<T> ret = new ArrayList<>();
        for (MapleMapObject object : getMapObjects()) {
            if (object.getClass() == t) {
                ret.add((T) object);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInBox(Rectangle box, List<MapleMapObjectType> types) {
        List<MapleMapObject> ret = new LinkedList<>();
        for (MapleMapObject l : getMapObjects()) {
            if (types.contains(l.getType())) {
                if (box.contains(l.getPosition())) {
                    ret.add(l);
                }
            }
        }
        return ret;
    }

    public void addPortal(MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        for (MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public MaplePortal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<>(areas);
    }

    public Rectangle getArea(int index) {
        return areas.get(index);
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public void addMonsterSpawn(MapleMonster monster, int mobTime, int team) {
        Point newpos = calcPointBelow(monster.getPosition());
        if (newpos == null) {
            LOGGER.warn("No foothold for monster {} at x:{},y:{} in map {}", monster.getId(), monster.getPosition().x, monster.getPosition().y, getId());
            return;
        }
        newpos.y -= 1;
        monster.setPosition(newpos);
        SpawnPoint spawnPoint = new SpawnPoint(this, monster, !monster.isMobile(), mobTime, team);
        MapleMonster summon = spawnPoint.getMonster();
        if (summon != null) {
            spawnPoint.summonMonster();
            spawnPoints.add(spawnPoint);
        } else {
            LOGGER.info("Unable to summon invalid monster {} in map {} via SpawnPoint", monster.getId(), getId());
        }
    }

    public void addMonsterSpawnPoint(SpawnPoint spawnPoint) {
        spawnPoints.add(spawnPoint);
    }

    public ArrayList<SpawnPoint> getMonsterSpawnPoints() {
        return new ArrayList<>(spawnPoints);
    }

    public MapleCharacter getCharacterById(int id) {
        for (MapleCharacter c : getCharacters()) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    private void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
        if (chr instanceof FakePlayer) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= 722500) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > 722500) {
            chr.removeVisibleMapObject(mo);
            mo.sendDestroyData(chr.getClient());
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        for (MapleCharacter chr : getCharacters()) {
            updateMapObjectVisibility(chr, monster);
        }
    }

    public void movePlayer(MapleCharacter player, Point newPosition) {
        player.setPosition(newPosition);
        Collection<MapleMapObject> visibleObjects = player.getVisibleMapObjects();
        MapleMapObject[] visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[visibleObjects.size()]);
        for (MapleMapObject mo : visibleObjectsNow) {
            if (mo != null) {
                if (mapobjects.get(mo.getObjectId()) == mo) {
                    updateMapObjectVisibility(player, mo);
                } else {
                    player.removeVisibleMapObject(mo);
                }
            }
        }
        for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), 722500, rangedMapobjectTypes)) {
            if (!player.isMapObjectVisible(mo)) {
                mo.sendSpawnData(player.getClient());
                player.addVisibleMapObject(mo);
            }
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            double distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean hasClock() {
        return clock;
    }

    public boolean isTown() {
        return town;
    }

    public void setTown(boolean isTown) {
        this.town = isTown;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean mute) {
        isMuted = mute;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public AtomicInteger getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap;
    }

    public void setMobCapacity(int capacity) {
        this.mobCapacity = capacity;
    }

    public void setBackgroundTypes(HashMap<Integer, Integer> backTypes) {
        backgroundTypes.putAll(backTypes);
    }

    // not really costly to keep generating imo
    public void sendNightEffect(MapleCharacter mc) {
        for (Entry<Integer, Integer> types : backgroundTypes.entrySet()) {
            if (types.getValue() >= 3) { // 3 is a special number
                mc.announce(MaplePacketCreator.changeBackgroundEffect(true, types.getKey(), 0));
            }
        }
    }

    public void broadcastNightEffect() {
        getCharacters().forEach(this::sendNightEffect);
    }

    public MapleCharacter getCharacterByName(String name) {
        for (MapleCharacter c : getCharacters()) {
            if (c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    public void instanceMapRespawn() {
        final int numShouldSpawn = (short) ((spawnPoints.size() - spawnedMonstersOnMap.get()));
        if (numShouldSpawn > 0) {
            List<SpawnPoint> randomSpawn = new ArrayList<>(spawnPoints);
            Collections.shuffle(randomSpawn);
            int spawned = 0;
            for (SpawnPoint spawnPoint : randomSpawn) {
                spawnMonster(spawnPoint.getMonster());
                spawned++;
                if (spawned >= numShouldSpawn) {
                    break;
                }
            }
        }
    }

    public void respawn() {
        if (!isRespawnEnabled()) {
            return;
        } else if (characters.isEmpty()) {
            return;
        }
        int respawns = (spawnPoints.size() - spawnedMonstersOnMap.get());
        if (respawns > 0) {
            List<SpawnPoint> spawns = spawnPoints.stream().filter(s -> s.canSpawn(true)).collect(Collectors.toList());
            for (SpawnPoint spawn : spawns) {
                if (spawn.canSpawn(true)) {
                    spawn.getMonster();
                    spawn.summonMonster();
                }
            }
        }
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        decHP = delta;
    }

    public int getHPDecProtect() {
        return protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    private int hasBoat() {
        return docked ? 2 : (boat ? 1 : 0);
    }

    public void setBoat(boolean hasBoat) {
        this.boat = hasBoat;
    }

    public void setDocked(boolean isDocked) {
        this.docked = isDocked;
    }

    public void broadcastGMMessage(MapleCharacter source, final byte[] packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(MapleCharacter source, final byte[] packet, double rangeSq, Point rangedFrom) {
        for (MapleCharacter chr : getCharacters()) {
            if (chr != source && chr.isGM()) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                        chr.getClient().announce(packet);
                    }
                } else {
                    chr.getClient().announce(packet);
                }
            }
        }
    }

    public void broadcastNONGMMessage(MapleCharacter source, final byte[] packet, boolean repeatToSource) {
        for (MapleCharacter chr : getCharacters()) {
            if (chr != source && !chr.isGM()) {
                chr.getClient().announce(packet);
            }
        }
    }

    public MapleOxQuiz getOx() {
        return ox;
    }

    public void setOx(MapleOxQuiz set) {
        this.ox = set;
    }

    public boolean isOxQuiz() {
        return isOxQuiz;
    }

    public void setOxQuiz(boolean b) {
        this.isOxQuiz = b;
    }

    public boolean isRespawnEnabled() {
        return respawnEnabled;
    }

    public void setRespawnEnabled(boolean respawnEnabled) {
        this.respawnEnabled = respawnEnabled;
    }

    public boolean isInstanced() {
        return instanced;
    }

    public void setInstanced(boolean instanced) {
        this.instanced = instanced;
    }

    public String getOnUserEnter() {
        return onUserEnter;
    }

    public void setOnUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public String getOnFirstUserEnter() {
        return onFirstUserEnter;
    }

    public void setOnFirstUserEnter(String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    private boolean hasForcedEquip() {
        return fieldType == 81 || fieldType == 82;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public void clearDrops(MapleCharacter player) {
        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.ITEM));
        for (MapleMapObject i : items) {
            player.getMap().removeMapObject(i);
            player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
        }
    }

    public void clearDrops() {
        for (MapleMapObject i : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.ITEM))) {
            removeMapObject(i);
            this.broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, 0));
        }
    }

    public void addMapTimer(int time) {
        timeLimit = System.currentTimeMillis() + (time * 1000);
        broadcastMessage(MaplePacketCreator.getClock(time));
        mapMonitor = TaskExecutor.createRepeatingTask(new Runnable() {
            @Override
            public void run() {
                if (timeLimit != 0 && timeLimit < System.currentTimeMillis()) {
                    warpEveryone(getForcedReturnId());
                }
                if (getCharacters().isEmpty()) {
                    resetReactors();
                    killAllMonsters();
                    clearDrops();
                    timeLimit = 0;
                    if (mapid >= 922240100 && mapid <= 922240119) {
                        toggleHiddenNPC(9001108);
                    }
                    mapMonitor.cancel();
                    mapMonitor = null;
                }
            }
        }, 1000);
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public void resetRiceCakes() {
        this.riceCakes = 0;
    }

    public boolean getSummonState() {
        return summonState;
    }

    public void setSummonState(boolean summonState) {
        this.summonState = summonState;
    }

    public void warpEveryone(int to) {
        getCharacters().forEach(p -> p.changeMap(to));
    }

    // BEGIN EVENTS
    public void setSnowball(int team, MapleSnowball ball) {
        switch (team) {
            case 0:
                this.snowball0 = ball;
                break;
            case 1:
                this.snowball1 = ball;
                break;
            default:
                break;
        }
    }

    public MapleSnowball getSnowball(int team) {
        switch (team) {
            case 0:
                return snowball0;
            case 1:
                return snowball1;
            default:
                return null;
        }
    }

    private boolean specialEquip() {// Maybe I shouldn't use fieldType :\
        return fieldType == 4 || fieldType == 19;
    }

    public MapleCoconut getCoconut() {
        return coconut;
    }

    public void setCoconut(MapleCoconut nut) {
        this.coconut = nut;
    }

    public void warpOutByTeam(int team, int mapid) {
        List<MapleCharacter> chars = new ArrayList<>(getCharacters());

        for (MapleCharacter chr : chars) {
            if (chr != null) {
                if (chr.getTeam() == team) {
                    chr.changeMap(mapid);
                }
            }
        }
    }

    public void startEvent(final MapleCharacter chr) {
        if (this.mapid == 109080000 && getCoconut() == null) {
            setCoconut(new MapleCoconut(this));
            coconut.startEvent();
        } else if (this.mapid == 109040000) {
            chr.setFitness(new MapleFitness(chr));
            chr.getFitness().startFitness();
        } else if (this.mapid == 109030101 || this.mapid == 109030201 || this.mapid == 109030301 || this.mapid == 109030401) {
            chr.setOla(new MapleOla(chr));
            chr.getOla().startOla();
        } else if (this.mapid == 109020001 && getOx() == null) {
            setOx(new MapleOxQuiz(this));
            getOx().sendQuestion();
            setOxQuiz(true);
        } else if (this.mapid == 109060000 && getSnowball(chr.getTeam()) == null) {
            setSnowball(0, new MapleSnowball(0, this));
            setSnowball(1, new MapleSnowball(1, this));
            getSnowball(chr.getTeam()).startEvent();
        }
    }

    public boolean eventStarted() {
        return eventstarted;
    }

    public void startEvent() {
        this.eventstarted = true;
    }

    public void setEventStarted(boolean event) {
        this.eventstarted = event;
    }

    public String getEventNPC() {
        StringBuilder sb = new StringBuilder();
        sb.append("Talk to ");
        if (mapid == 60000) {
            sb.append("Paul!");
        } else if (mapid == 104000000) {
            sb.append("Jean!");
        } else if (mapid == 200000000) {
            sb.append("Martin!");
        } else if (mapid == 220000000) {
            sb.append("Tony!");
        } else {
            return null;
        }
        return sb.toString();
    }

    public boolean hasEventNPC() {
        return this.mapid == 60000 || this.mapid == 104000000 || this.mapid == 200000000 || this.mapid == 220000000;
    }

    public boolean isStartingEventMap() {
        return this.mapid == 109040000 || this.mapid == 109020001 || this.mapid == 109010000 || this.mapid == 109030001 || this.mapid == 109030101;
    }

    public boolean isEventMap() {
        return this.mapid >= 109010000 && this.mapid < 109050000 || this.mapid > 109050001 && this.mapid <= 109090000;
    }

    public void timeMob(int id, String msg) {
        timeMob = new Pair<>(id, msg);
    }

    public Pair<Integer, String> getTimeMob() {
        return timeMob;
    }

    public void toggleHiddenNPC(int id) {
        for (MapleMapObject obj : mapobjects.values()) {
            if (obj.getType() == MapleMapObjectType.NPC) {
                MapleNPC npc = (MapleNPC) obj;
                if (npc.getId() == id) {
                    npc.setHide(!npc.isHidden());
                    if (!npc.isHidden()) // Should only be hidden upon changing
                    // maps
                    {
                        broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                    }
                }
            }
        }
    }

    public short getMobInterval() {
        return mobInterval;
    }

    public void setMobInterval(short interval) {
        this.mobInterval = interval;
    }

    public byte getMonsterRate() {
        return monsterRate;
    }

    public void setMonsterRate(byte monsterRate) {
        this.monsterRate = monsterRate;
    }

    public Point getAutoKillPosition() {
        return autoKillPosition;
    }

    public void setAutoKillPosition(Point autoKillPosition) {
        this.autoKillPosition = autoKillPosition;
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    private static interface SpawnCondition {

        boolean canSpawn(MapleCharacter chr);
    }

    private class ExpireMapItemJob implements Runnable {

        private MapleMapItem mapitem;

        public ExpireMapItemJob(MapleMapItem mapitem) {
            this.mapitem = mapitem;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
                mapitem.itemLock.lock();
                try {
                    if (mapitem.isPickedUp()) {
                        return;
                    }
                    MapleMap.this.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                    mapitem.setPickedUp(true);
                } finally {
                    mapitem.itemLock.unlock();
                    MapleMap.this.removeMapObject(mapitem);
                }
            }
        }
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapItem;
        private MapleReactor reactor;
        private MapleClient client;

        public ActivateItemReactor(MapleClient client, MapleMapItem mapItem, MapleReactor reactor) {
            this.client = client;
            this.mapItem = mapItem;
            this.reactor = reactor;
        }

        @Override
        public void run() {
            if (mapItem != null && mapItem == getMapObject(mapItem.getObjectId())) {
                mapItem.itemLock.lock();
                try {
                    if (mapItem.isPickedUp()) {
                        return;
                    }
                    if (client.getPlayer().isDebug()) {
                        client.getPlayer().sendMessage("Reactor Activated ID {}, Name {}, State {}", reactor.getId(), reactor.getName(), reactor.getState());
                    }
                    broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 0, 0), mapItem.getPosition());
                    removeMapObject(mapItem);
                    reactor.hitReactor(client);
                    reactor.setTimerActive(false);
                    if (reactor.getDelay() > 0) {
                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                reactor.setState((byte) 0);
                                broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
                            }
                        }, reactor.getDelay());
                    }
                } finally {
                    mapItem.itemLock.unlock();
                }
            }
        }
    }
}
