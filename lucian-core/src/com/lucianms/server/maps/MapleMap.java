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
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.ItemConstants;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
import com.lucianms.events.gm.MapleFitness;
import com.lucianms.events.gm.MapleOla;
import com.lucianms.events.gm.MapleOxQuiz;
import com.lucianms.events.gm.MapleSnowball;
import com.lucianms.features.carnival.MCarnivalMonsterLocation;
import com.lucianms.features.carnival.MonsterCarnival;
import com.lucianms.features.coconut.CoconutEvent;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.io.scripting.map.FieldScriptExecutor;
import com.lucianms.lang.GProperties;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.*;
import com.lucianms.server.life.*;
import com.lucianms.server.life.MapleLifeFactory.SelfDestruction;
import com.lucianms.server.partyquest.Pyramid;
import com.lucianms.server.world.MapleParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.tools.EffectProvider;
import tools.MaplePacketCreator;
import tools.PacketAnnouncer;
import tools.Pair;
import tools.Randomizer;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapleMap implements PacketAnnouncer {

    public static final int RESPAWN_INTERVAL = 5000;
    public static final int MAX_DROP_CHANCE = 1000000;
    public static final int INVALID_ID = 999999999;
    public static final String AUTO_KILL_TOGGLE = "auto_kill_toggle";
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
    private int decHP;
    private int fieldType;
    private int returnMapId;
    private int channel, world;
    private int fieldLimit;
    private int protectItem;
    private int mobCapacity = -1;
    private int forcedReturnMap = 999999999;
    private short mobInterval = 5000;
    private byte monsterRate;
    private boolean boat;
    private boolean town;
    private boolean clock;
    private boolean docked;
    private boolean swimEnabled;
    private boolean dropsOn = true;
    private boolean everlast;
    private boolean isOxQuiz;
    private boolean summonState = true; // All maps should have this true at the beginning
    private boolean respawnEnabled = true;
    private boolean instanced;
    private MapleMapEffect mapEffect;
    private MapleOxQuiz ox;
    private Task mapMonitor;
    private Pair<Integer, String> timeMob;
    private long nextEmergency;
    private String lastPlayerDiedInMap = ""; // THIS IS HOW YOU'RE SUPPOSED TO FUCKING DO VARIABLES - also no nullpointers >:(

    //region Henesys PQ
    private int riceCakes;
    private int bunnyDamage;
    //endregion

    //region events
    private int tagRange;
    private boolean eventStarted;
    private boolean muted;
    private MapleSnowball snowball0, snowball1;
    private CoconutEvent coconut;
    private MonsterCarnival monsterCarnival;
    private GProperties<Point> autoKillPositions = new GProperties<>();
    private GProperties<Boolean> autoKillMobs = new GProperties<>();
    private GProperties<Object> variables = new GProperties<>();
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

        tagRange = 20000;
    }

    @Override
    public Collection<MapleCharacter> getPlayers() {
        // this is so bad but oh well odin is trash
        return new ArrayList<>(getCharacters());
    }

    public static void doItemVac(MapleCharacter player, MaplePet pet, int range) {
        List<MapleMapItem> mapDrops = player.getMap().getMapObjects().stream()
                .filter(o -> o instanceof MapleMapItem)
                .map(o -> (MapleMapItem) o)
                .collect(Collectors.toList());
        Point origin = (pet == null) ? player.getPosition() : pet.getPos();

        for (MapleMapItem drop : mapDrops) {
            if (range != -1) {
                double distance = drop.getPosition().distance(origin);
                if (distance > range) {
                    continue;
                }
            }
            if (drop.getMeso() > 0) {
                player.gainMeso(drop.getMeso(), true);
            } else if (drop.getItemId() == 4031865 || drop.getItemId() == 4031866) {
                // Add NX to account, show effect and make item disapear
                player.getCashShop().gainCash(1, drop.getItemId() == 4031865 ? 100 : 250);
            } else {
                MapleInventoryManipulator.addFromDrop(player.getClient(), drop.getItem(), true);
            }
            drop.setPickedUp(true);
            if (pet != null) {
                player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(drop.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)));
            } else {
                player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(drop.getObjectId(), 2, player.getId()), drop.getPosition());
            }
            player.getMap().removeMapObject(drop);
        }
        mapDrops.clear();
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
                .filter(c -> c.getId() != source.getId() && c.getGMLevel() > source.getGMLevel())
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
            if (chr.getPartyID() != -1) {
                return chr.getPartyID();
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

        ArrayList<MonsterDropEntry> dropEntry = new ArrayList<>(MapleMonsterInformationProvider.retrieveDrop(mob.getId()));

        for (MonsterListener listener : mob.getListeners()) {
            MonsterDropEntry drop = listener.onDeathDrop(mob, chr);
            if (drop != null) {
                dropEntry.add(drop);
            }
        }

        for (CQuestData qData : chr.getCustomQuests().values()) {
            if (!qData.isCompleted()) {
                for (CQuestItemRequirement.CQuestItem qItem : qData.getToCollect().getItems().values()) {
                    if (qItem.getMonsterId() == mob.getId()) {
                        int chance = (MAX_DROP_CHANCE / 1000) * qItem.getChance();
                        dropEntry.add(new MonsterDropEntry(qItem.getItemId(), chance, qItem.getMinQuantity(), qItem.getMaxQuantity(), (short) -1));
                    }
                }
            }
        }

        Collections.shuffle(dropEntry);
        for (MonsterDropEntry de : dropEntry) {
            if (de.shouldDrop(chServerrate)) {
                if (droptype == 3) {
                    pos.x = mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2)));
                } else {
                    pos.x = mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                }
                int amount = Randomizer.rand(de.Minimum, de.Maximum);
                if (de.itemId == 0) { // meso
                    if (amount > 0) {
                        if (chr.getBuffedValue(MapleBuffStat.MESO_UP) != null) {
                            amount = (int) (amount * chr.getBuffedValue(MapleBuffStat.MESO_UP).doubleValue() / 100.0);
                        }
                        spawnMesoDrop(amount * chr.getMesoRate(), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
                    }
                } else {
                    if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats(ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (short) 0, (short) amount);
                    }
                    spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid);
                }
                d++;
            }
        }
        dropEntry.clear();

        // Global Drops
        List<MonsterGlobalDropEntry> globalEntry = MapleMonsterInformationProvider.getGlobalDrop();
        for (MonsterGlobalDropEntry de : globalEntry) {
            float nRate = chServerrate;
            if (ItemConstants.isEyeScanner(de.itemId)) {
                nRate *= 0.1;
            }
            if (de.shouldDrop(nRate)) {
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

    }

    public final MapleMapItem disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
        return drop;
    }

    public MapleMonster getMonsterById(int id) {
        Collection<MapleMapObject> mapObjects = new ArrayList<>(getMapObjects());
        try {
            for (MapleMapObject obj : mapObjects) {
                if (obj.getType() == MapleMapObjectType.MONSTER) {
                    if (((MapleMonster) obj).getId() == id) {
                        return (MapleMonster) obj;
                    }
                }
            }
            return null;
        } finally {
            mapObjects.clear();
        }
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

    public boolean damageMonster(final MapleCharacter chr, final MapleMonster monster, final long damage) {
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
                        MapleParty party = chr.getParty();
                        if (getId() == 85) {
                            if (party != null) {
                                for (MapleCharacter players : getAllPlayer()) {
                                    players.changeMap(88);
                                    players.dropMessage(5, "Thanks for finally letting me realize my actions were corrupt against the realm.");
                                }
                            } else {
                                chr.changeMap(88);
                                chr.dropMessage(5, "Thanks for finally letting me realize my actions were corrupt against the realm.");
                            }
                        } else if (getId() == 551030901 && monster.getId() == 9989996) {
                            TaskExecutor.createTask(() -> warpEveryone(551030902), 3000);
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
        spawnedMonstersOnMap.decrementAndGet();
        if (chr == null) {
            monster.killBy(null);
            monster.setHp(0);
            sendPacket(MaplePacketCreator.killMonster(monster.getObjectId(), animation));
            removeMapObject(monster);
            return;
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
            chr.getClient().getWorldServer().sendMessage(6, "To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
        }
        if (monster.getId() == 9895253 && getId() == 97) { // Black Mage
            List<MapleCharacter> warp = new ArrayList<>(getCharacters());
            TaskExecutor.createTask(() -> warp.forEach(c -> c.changeMap(333)), 2500);
            warp.clear();
        }
        monster.setHp(0);
        sendPacket(MaplePacketCreator.killMonster(monster.getObjectId(), animation));
        removeMapObject(monster);
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
        Collection<MapleMapObject> mapObjects = new ArrayList<>(getMapObjects());
        try {
            for (MapleMapObject mmo : mapObjects) {
                if (mmo instanceof MapleMonster) {
                    if (((MapleMonster) mmo).getId() == monsId) {
                        this.killMonster((MapleMonster) mmo, getAllPlayer().get(0), false);
                    }
                }
            }
        } finally {
            mapObjects.clear();
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
        spawnedMonstersOnMap.set(0);
        for (MapleMonster monster : getMapObjects(MapleMonster.class)) {
            monster.setHp(0);
            monster.killBy(null);
            broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), true));
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
        if (!monster.isAlive()) {
            return;
        }
        monster.monsterLock.lock();
        try {
            MapleCharacter controller = monster.getController();
            if (controller != null) {
                if (controller.getMap() != this) {
                    controller.stopControllingMonster(monster);
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

    public Collection<MapleCharacter> getCharacters() {
        return characters.values();
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

    public void addCarnivalMonster(MapleMonster monster, int team) {
        if (monsterCarnival == null) {
            LOGGER.warn("Cannot summon Monster Carnival due to invalid carnival map");
            return;
        }
        Optional<MCarnivalMonsterLocation> found;
        Stream<MCarnivalMonsterLocation> stream = monsterCarnival.getMonsterGenLocations().stream();
        if (monsterCarnival.isMapDivded()) {
            found = stream.filter(l -> l.getTeam() == team).findAny();
        } else {
            found = stream.findAny();
        }
        if (!found.isPresent()) {
            LOGGER.warn("Cannot summon Monster Carnival mob because there are no gen locations");
            return;
        }
        MCarnivalMonsterLocation location = found.get();
        monster.setPosition(new Point(location.getX(), location.getY()));
        monster.setCy(location.getCy());
        monster.setFoothold(location.getFoothold());
        SpawnPoint spawnPoint = new SpawnPoint(this, monster, false, 1, team);
        addMonsterSpawnPoint(spawnPoint);

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
        if (mob.getSummonEffect() > 0) {
            spawnMonsterWithEffect(mob, mob.getSummonEffect(), pos);
            return;
        }
        Point newLocation = new Point(pos.x, pos.y - 5);
        mob.setPosition(newLocation);
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
        if (mobCapacity != -1 && spawnedMonstersOnMap.get() >= mobCapacity) {
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
                LOGGER.warn("Unhandled timed monster {}", monster.getId());
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
        Point newLocation = new Point(pos.x, pos.y - 3);
        monster.setPosition(newLocation);
        if (mapid < 925020000 || mapid > 925030000) {
            monster.disableDrops();
        }
        spawnAndAddRangedMapObject(monster, c -> c.announce(MaplePacketCreator.spawnMonster(monster, true, effect)));
        if (monster.hasBossHPBar()) {
            broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
        }
        int summonEffect = EffectProvider.getSummonEffect(effect);
        if (summonEffect > 0) {
            // uhh don't waste the thread-pool i guess?
            TaskExecutor.createTask(() -> updateMonsterController(monster), summonEffect);
        } else {
            updateMonsterController(monster);
        }

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
                if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsKey(c.getPlayer().getId()))) {
                    c.announce(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                }
                c.announce(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
                c.announce(MaplePacketCreator.enableActions());
            }
        }, chr -> chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null);

    }

    public List<MapleCharacter> getPlayersInRange(Rectangle box, List<MapleCharacter> chr) {
        List<MapleCharacter> inRange = new ArrayList<>();
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
                            if (mist.getOwner().getId() == chr.getId() || mist.getOwner().getParty() != null && mist.getOwner().getParty().containsKey(chr.getId())) {
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
        addMapObject(fakePlayer);
        fakePlayer.setId(fakePlayer.getObjectId());
        characters.put(fakePlayer.getId(), fakePlayer);
        broadcastMessage(fakePlayer, MaplePacketCreator.getUserEnterField(fakePlayer), false);
    }

    public void removeFakePlayer(FakePlayer fakePlayer) {
        removeMapObject(fakePlayer.getObjectId());
        characters.remove(fakePlayer.getId());
        broadcastMessage(fakePlayer, MaplePacketCreator.removePlayerFromMap(fakePlayer.getId()), false);
    }

    public void addPlayer(final MapleCharacter player) {
        characters.put(player.getId(), player);

        player.setMapId(mapid);

        //region script processing
        if (onFirstUserEnter.length() != 0 && !player.hasEntered(onFirstUserEnter, mapid)) {
            if (getAllPlayer().size() <= 1) {
                player.enteredScript(onFirstUserEnter, mapid);
                FieldScriptExecutor.executeFirstEnter(player.getClient(), onFirstUserEnter);
            }
        }
        if (onUserEnter != null && !onUserEnter.isEmpty()) {
            if (onUserEnter.equals("cygnusTest") && (mapid < 913040000 || mapid > 913040006)) {
                player.saveLocation("INTRO");
            }
            FieldScriptExecutor.executeEnter(player.getClient(), onUserEnter);
        } else {
            String strMapID = Integer.toString(mapid);
            FieldScriptExecutor.executeEnter(player.getClient(), strMapID);
        }
        //endregion

        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && player.getBuffedValue(MapleBuffStat.RIDE_VEHICLE) != null) {
            player.cancelBuffs(Set.of(MapleBuffStat.RIDE_VEHICLE));
            player.cancelBuffStats(MapleBuffStat.RIDE_VEHICLE);
        }
        if (mapid == 923010000 && getMonsterById(9300102) == null) { // Kenta's Mount quest
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300102), new Point(77, 426));
        }

        //region map timeouts
        if (mapid == 910010200) { // Henesys Party Quest Bonus
            player.announce(MaplePacketCreator.getClock(60 * 5));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 910010200) {
                        player.changeMap(910010400);
                    }
                }
            }, 5 * 60 * 1000);
        } else if (mapid == 200090060) { // To Rien
            player.announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090060) {
                        player.changeMap(140020300);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090070) { // To Lith Harbor
            player.announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090070) {
                        player.changeMap(104000000, 3);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090030) { // To Ereve (SkyFerry)
            player.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090030) {
                        player.changeMap(130000210);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090031) { // To Victoria Island (SkyFerry)
            player.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090031) {
                        player.changeMap(101000400);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090021) { // To Orbis (SkyFerry)
            player.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090021) {
                        player.changeMap(200000161);
                    }
                }
            }, 60 * 1000);
        } else if (mapid == 200090020) { // To Ereve From Orbis (SkyFerry)
            player.getClient().announce(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    if (player.getMapId() == 200090020) {
                        player.changeMap(130000210);
                    }
                }
            }, 60 * 1000);
        }
        //endregion

        EventInstanceManager eim = player.getEventInstance();

        if (mapid == 103040400) {
            if (eim != null) {
                eim.movePlayer(player, this);
            }
        }
        if (coconut != null) {
            player.setTeam(characters.size() % 2 == 0 ? 1 : 0);
            player.announce(MaplePacketCreator.getCoconutScore(0, 0));
        }
        if (specialEquip()) {
            player.announce(MaplePacketCreator.getUpdateFieldSpecificData(player.getTeam()));
        }
        MaplePet[] pets = player.getPets();
        for (int i = 0; i < player.getPets().length; i++) {
            if (pets[i] != null) {
                Point nPosition = getGroundBelow(player.getPosition());
                if (nPosition == null) {
                    nPosition = player.getPosition().getLocation();
                }
                pets[i].setPos(nPosition);
                player.announce(MaplePacketCreator.showPet(player, pets[i], false, false));
            } else {
                break;
            }
        }
        if (player.isHidden()) {
            sendPacketIf(MaplePacketCreator.getUserEnterField(player), p -> p.getGMLevel() >= player.getGMLevel());
            player.announce(MaplePacketCreator.getAdminResult(0x10, (byte) 1));

            sendPacketIf(MaplePacketCreator.giveForeignBuff(getId(), Map.of(MapleBuffStat.DARK_SIGHT, 0)), p -> p.getGMLevel() >= player.getGMLevel());
        } else {
            broadcastMessage(player, MaplePacketCreator.getUserEnterField(player), false);
        }

        sendObjectPlacement(player.getClient());
        addMapObject(player);

        if (isStartingEventMap() && !isEventStarted()) {
            player.getMap().getPortal("join00").setPortalStatus(false);
        }
        if (hasForcedEquip()) {
            player.getClient().announce(MaplePacketCreator.getUpdateFieldSpecificData(-1));
        }
        if (player.getPlayerShop() != null) {
            addMapObject(player.getPlayerShop());
        }

        final MapleDragon dragon = player.getDragon();
        if (dragon != null) {
            dragon.setPosition(player.getPosition());
            addMapObject(dragon);
            if (player.isHidden()) {
                broadcastGMMessage(player, MaplePacketCreator.spawnDragon(dragon));
            } else {
                broadcastMessage(player, MaplePacketCreator.spawnDragon(dragon));
            }
        } else if (GameConstants.hasSPTable(player.getJob())) {
            player.createDragon();
            broadcastMessage(MaplePacketCreator.spawnDragon(player.getDragon()));
        }

        for (MapleSummon summon : player.getSummons().values()) {
            summon.setPosition(player.getPosition());
            player.getMap().spawnSummon(summon);
            updateMapObjectVisibility(player, summon);
        }

        if (hasBoat() == 2) {
            player.getClient().announce((MaplePacketCreator.boatPacket(true)));
        } else if (hasBoat() == 1 && (player.getMapId() != 200090000 || player.getMapId() != 200090010)) {
            player.getClient().announce(MaplePacketCreator.boatPacket(false));
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
        characters.remove(chr.getId());
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
            removeMapObject(summon);
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

    //region message broadcast
    public void broadcastMessage(int type, String message, Object... args) {
        getCharacters().forEach(p -> p.sendMessage(type, message, args));
    }

    public void broadcastGMMessage(int type, String message, Object... args) {
        getCharacters().stream().filter(MapleCharacter::isGM).forEach(p -> p.sendMessage(type, message, args));
    }
    //endregion

    //region packet broadcast
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
    //endregion

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
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        Iterator<MapleMapObject> iterator = getMapObjects().iterator();
        while (iterator.hasNext()) {
            MapleMapObject ent = iterator.next();
            if (types.contains(ent.getType())) {
                if (from.distanceSq(ent.getPosition()) <= rangeSq) {
                    ret.add(ent);
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
            if (object.getClass().isAssignableFrom(t)) {
                ret.add((T) object);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInBox(Rectangle box, List<MapleMapObjectType> types) {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        ArrayList<MapleMapObject> objects = getMapObjects(MapleMapObject.class);
        try {
            for (MapleMapObject l : objects) {
                if (types.contains(l.getType())) {
                    if (box.contains(l.getPosition())) {
                        ret.add(l);
                    }
                }
            }
        } finally {
            objects.clear();
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

    public MaplePortal getPortal(int portalID) {
        return portals.get(portalID);
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
            LOGGER.warn("No foothold for monster {} at [{}, {}] map {}", monster.getId(), monster.getPosition().x, monster.getPosition().y, getId());
            return;
        }
        newpos.y -= 1;
        monster.setPosition(newpos);
        spawnPoints.add(new SpawnPoint(this, monster, !monster.isMobile(), mobTime, team));
    }

    public void addMonsterSpawnPoint(SpawnPoint spawnPoint) {
        spawnPoints.add(spawnPoint);
    }

    public ArrayList<SpawnPoint> getMonsterSpawnPoints() {
        return new ArrayList<>(spawnPoints);
    }

    public MapleCharacter getCharacterById(int id) {
        return characters.get(id);
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
        ArrayList<MapleMapObject> visibleObjects = new ArrayList<>(player.getVisibleMapObjects());
        MapleMapObject[] visibleObjectsNow = visibleObjects.toArray(new MapleMapObject[0]);
        for (MapleMapObject mo : visibleObjectsNow) {
            if (mo != null) {
                if (mapobjects.get(mo.getObjectId()) == mo) {
                    updateMapObjectVisibility(player, mo);
                } else {
                    player.removeVisibleMapObject(mo);
                }
            }
        }
        visibleObjects.clear();
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
        return muted;
    }

    public void setMuted(boolean mute) {
        muted = mute;
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

    public int getMobCapacity() {
        return mobCapacity;
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

    public void respawn() {
        if (!isRespawnEnabled()) {
            return;
        } else if (characters.isEmpty()) {
            return;
        }
        spawnPoints.forEach(SpawnPoint::attemptMonsterSummon);
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

    public boolean isSwimEnabled() {
        return swimEnabled;
    }

    public void setSwimEnabled(boolean swimEnabled) {
        this.swimEnabled = swimEnabled;
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
        items.clear();
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
        ArrayList<MapleCharacter> characters = new ArrayList<>(getCharacters());
        characters.forEach(p -> p.changeMap(to));
        characters.clear();
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

    private boolean specialEquip() {
        return fieldType == 4 || fieldType == 19;
    }

    public CoconutEvent getCoconut() {
        return coconut;
    }

    public void setCoconut(CoconutEvent nut) {
        this.coconut = nut;
    }

    public MonsterCarnival getMonsterCarnival() {
        return monsterCarnival;
    }

    public void setMonsterCarnival(MonsterCarnival monsterCarnival) {
        this.monsterCarnival = monsterCarnival;
    }

    public String getLastPlayerDiedInMap() {
        return lastPlayerDiedInMap;
    }

    public void setLastPlayerDiedInMap(String lastPlayerDiedInMap) {
        this.lastPlayerDiedInMap = lastPlayerDiedInMap;
    }

    public void warpOutByTeam(int team, int mapid) {
        List<MapleCharacter> chars = new ArrayList<>(getCharacters());
        try {
            for (MapleCharacter chr : chars) {
                if (chr != null) {
                    if (chr.getTeam() == team) {
                        chr.changeMap(mapid);
                    }
                }
            }
        } finally {
            chars.clear();
        }
    }

    public void startEvent(final MapleCharacter chr) {
        if (this.mapid == 109040000) {
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

    public boolean isEventStarted() {
        return eventStarted;
    }

    public void setEventStarted(boolean event) {
        this.eventStarted = event;
    }

    public int getTagRange() {
        return tagRange;
    }

    public void setTagRange(int tagRange) {
        this.tagRange = tagRange;
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
                    if (!npc.isHidden()) { // Should only be hidden upon changing maps
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

    public GProperties<Point> getAutoKillPositions() {
        return autoKillPositions;
    }

    public GProperties<Boolean> getAutoKillMobs() {
        return autoKillMobs;
    }

    public GProperties<Object> getVariables() {
        return variables;
    }

    @Deprecated
    private interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    @Deprecated
    private interface SpawnCondition {

        boolean canSpawn(MapleCharacter chr);
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
