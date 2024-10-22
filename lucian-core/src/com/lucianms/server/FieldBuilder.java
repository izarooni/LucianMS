package com.lucianms.server;

import com.lucianms.constants.GameConstants;
import com.lucianms.features.carnival.MCarnivalGuardian;
import com.lucianms.features.carnival.MCarnivalMonster;
import com.lucianms.features.carnival.MCarnivalMonsterLocation;
import com.lucianms.features.carnival.MonsterCarnival;
import com.lucianms.features.coconut.CoconutEvent;
import com.lucianms.features.coconut.CoconutObject;
import com.lucianms.server.life.*;
import com.lucianms.server.maps.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.StringUtil;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author izarooni
 */
public class FieldBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBuilder.class);
    private static final MapleDataProvider source = MapleDataProviderFactory.getWZ(new File(System.getProperty("wzpath") + "/Map.wz"));
    private static final MapleData nameData = MapleDataProviderFactory.getWZ(new File(System.getProperty("wzpath") + "/String.wz")).getData("Map.img");

    private MapleData mapData;
    private MapleMap map;
    private boolean portals = false;
    private boolean footholds = false;
    private boolean reactors = false;
    private boolean monsters = false;
    private boolean NPCs = false;
    private boolean playerNPCs = false;
    private boolean SQLSpawns = false;

    public FieldBuilder(int worldID, int channelID, int fieldID) {
        map = new MapleMap(fieldID, worldID, channelID, 0, 0);
    }

    //region builder toggles
    public FieldBuilder loadAll() {
        portals = true;
        footholds = true;
        reactors = true;
        monsters = true;
        NPCs = true;
        playerNPCs = true;
        SQLSpawns = true;
        return this;
    }

    public FieldBuilder loadPortals() {
        portals = !portals;
        return this;
    }

    public FieldBuilder loadFootholds() {
        footholds = !footholds;
        return this;
    }

    public FieldBuilder loadReactors() {
        reactors = !reactors;
        return this;
    }

    public FieldBuilder loadMonsters() {
        monsters = !monsters;
        return this;
    }

    public FieldBuilder loadNPCs() {
        NPCs = !NPCs;
        return this;
    }

    public FieldBuilder loadPlayerNPCs() {
        playerNPCs = !playerNPCs;
        return this;
    }

    public FieldBuilder loadSQLSpawns() {
        SQLSpawns = !SQLSpawns;
        return this;
    }
    //endregion

    private void obtainInformoation() {
        String link = MapleDataTool.getString(mapData.getChildByPath("info/link"), null);
        if (link != null) {
            mapData = source.getData(getMapName(Integer.parseInt(link)));
        }
        MapleData data;
        data = mapData.getChildByPath("info/mobRate");
        map.setMonsterRate((byte) ((data == null) ? 1 : Math.ceil((float) data.getData())));
        data = mapData.getChildByPath("info/timeMob");
        if (data != null) {
            map.timeMob(MapleDataTool.getInt(data.getChildByPath("id")), MapleDataTool.getString(data.getChildByPath("message")));
        }

        map.setHPDec(MapleDataTool.getIntConvert("info/decHP", mapData, 0));
        map.setReturnMapId(MapleDataTool.getInt("info/returnMap", mapData));
        map.setTown(MapleDataTool.getInt(mapData.getChildByPath("info/town"), 0) == 1);
        map.setFieldType(MapleDataTool.getIntConvert("info/fieldType", mapData, 0));
        map.setHPDecProtect(MapleDataTool.getIntConvert("info/protectItem", mapData, 0));
        map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
        map.setSwimEnabled(MapleDataTool.getInt(mapData.getChildByPath("info/swim"), 0) == 1);
        map.setOnUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), Integer.toString(map.getId())));
        map.setForcedReturnMap(MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
        map.setOnFirstUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), Integer.toString(map.getId())));
        map.setMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 5000));

        map.setBoat(mapData.getChildByPath("shipObj") != null);
        map.setEverlast(mapData.getChildByPath("everlast") != null);
        map.setTimeLimit(MapleDataTool.getIntConvert("timeLimit", mapData.getChildByPath("info"), -1));
        map.setMobCapacity(MapleDataTool.getIntConvert("fixedMobCapacity", mapData.getChildByPath("info"), 500));

        try {
            map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getNodePath(map.getId())), ""));
            map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getNodePath(map.getId())), ""));
        } catch (NullPointerException e) {
            map.setMapName("");
            map.setStreetName("");
        }

        HashMap<Integer, Integer> back = new HashMap<>();
        try {
            for (MapleData layer : mapData.getChildByPath("back")) {
                int layerNum = Integer.parseInt(layer.getName());
                int type = MapleDataTool.getInt(layer.getChildByPath("type"), 0);
                back.put(layerNum, type);
            }
        } catch (Exception ignore) {
        }

        MapleData coconutData = mapData.getChildByPath("coconut");
        if (coconutData != null) {
            CoconutEvent coconut = new CoconutEvent();
            coconut.setFalling(MapleDataTool.getIntConvert("countFalling", coconutData));
            coconut.setBombing(MapleDataTool.getIntConvert("countBombing", coconutData));
            coconut.setStopped(MapleDataTool.getIntConvert("countStopped", coconutData));
            coconut.setHit(MapleDataTool.getIntConvert("countHit", coconutData));
            coconut.setTimeDefault(MapleDataTool.getIntConvert("timeDefault", coconutData));
            coconut.setTimeExpand(MapleDataTool.getIntConvert("timeExpand", coconutData));
            coconut.setTimeFinish(MapleDataTool.getInt("timeFinish", coconutData));
            coconut.setEffectWin(MapleDataTool.getString("effectWin", coconutData));
            coconut.setEffectLose(MapleDataTool.getString("effectLose", coconutData));
            coconut.setSoundWin(MapleDataTool.getString("soundWin", coconutData));
            coconut.setSoundLose(MapleDataTool.getString("soundLose", coconutData));
            ArrayList<CoconutObject> coconuts = new ArrayList<>(505);
            coconut.setCoconuts(coconuts);
            for (int layer = 0; ; layer++) {
                MapleData objs = mapData.getChildByPath(Integer.toString(layer));
                if (objs == null || (objs = objs.getChildByPath("obj")) == null) break;
                for (MapleData obj : objs.getChildren()) {
                    String objPath = String.format("%s/%s/%s", MapleDataTool.getString("oS", obj),
                            MapleDataTool.getString("l0", obj),
                            MapleDataTool.getString("l1", obj));
                    if (objPath.equalsIgnoreCase("event/sandBeach/coconut")) {
                        String name = MapleDataTool.getString("name", obj);
                        Point pos = new Point(MapleDataTool.getIntConvert("x", obj), MapleDataTool.getIntConvert("y", obj));
                        coconuts.add(new CoconutObject(name, pos));
                    }
                }
            }
            map.setCoconut(coconut);
        }
        MapleData cpqData = mapData.getChildByPath("monsterCarnival");
        if (cpqData != null) {
            MonsterCarnival carnival = new MonsterCarnival();
            carnival.setDeathCP(MapleDataTool.getIntConvert("deathCP", cpqData));
            carnival.setEffectLose(MapleDataTool.getString("effectLose", cpqData, "quest/carnival/lose"));
            carnival.setEffectWin(MapleDataTool.getString("effectWin", cpqData, "quest/carnival/win"));
            carnival.setSoundLose(MapleDataTool.getString("soundLose", cpqData, "MobCarnival/Lose"));
            carnival.setSoundWin(MapleDataTool.getString("soundWin", cpqData, "MobCarnival/Win"));
            carnival.setGuardianGenMax(MapleDataTool.getIntConvert("guardianGenMax", cpqData, 8));
            List<MapleData> children = cpqData.getChildByPath("guardianGenPos").getChildren();
            ArrayList<MCarnivalGuardian> guardians = new ArrayList<>(children.size());
            for (MapleData child : children) {
                Point location = new Point(MapleDataTool.getIntConvert("x", child), MapleDataTool.getInt("y", child));
                boolean flipped = MapleDataTool.getIntConvert("f", child) != 0;
                MCarnivalGuardian guardian = new MCarnivalGuardian(location, flipped);
                guardian.setTeam(MapleDataTool.getIntConvert("team", child, 0));
                guardians.add(guardian);
            }
            carnival.setGuardians(guardians);
            children = cpqData.getChildByPath("mob").getChildren();
            ArrayList<MCarnivalMonster> monsters = new ArrayList<>(children.size());
            for (MapleData child : children) {
                monsters.add(new MCarnivalMonster(MapleDataTool.getIntConvert("id", child),
                        MapleDataTool.getIntConvert("mobTime", child),
                        MapleDataTool.getIntConvert("spendCP", child)));
            }
            carnival.setMonsters(monsters);
            carnival.setMonsterGenMax(MapleDataTool.getIntConvert("mobGenMax", cpqData, 10));
            children = cpqData.getChildByPath("mobGenPos").getChildren();
            ArrayList<MCarnivalMonsterLocation> locations = new ArrayList<>(children.size());
            for (MapleData child : children) {
                MCarnivalMonsterLocation location = new MCarnivalMonsterLocation(MapleDataTool.getIntConvert("x", child), MapleDataTool.getInt("y", child));
                location.setCy(MapleDataTool.getIntConvert("cy", child));
                location.setFoothold(MapleDataTool.getIntConvert("fh", child));
                location.setTeam(MapleDataTool.getIntConvert("team", child, 0));
                locations.add(location);
            }
            carnival.setMonsterGenLocations(locations);
            carnival.setMapDivded(MapleDataTool.getIntConvert("mapDivided", cpqData, 0) != 0);
            carnival.setReactorBlue(MapleDataTool.getIntConvert("reactorBlue", cpqData));
            carnival.setReactorRed(MapleDataTool.getIntConvert("reactorRed", cpqData));
            carnival.setLoserFieldID(MapleDataTool.getIntConvert("rewardMapLose", cpqData));
            carnival.setWinnerFieldID(MapleDataTool.getIntConvert("rewardMapWin", cpqData));
            carnival.setTimeDefault(MapleDataTool.getIntConvert("timeDefault", cpqData));
            carnival.setTimeFinish(MapleDataTool.getIntConvert("timeFinish", cpqData, 12));
            map.setMonsterCarnival(carnival);
        }
        map.setBackgroundTypes(back);
    }

    private void obtainPortals() {
        PortalFactory portalFactory = new PortalFactory();
        for (MapleData portal : mapData.getChildByPath("portal")) {
            map.addPortal(portalFactory.makePortal(MapleDataTool.getInt(portal.getChildByPath("pt")), portal));
        }
    }

    private void obtainFootholds() {
        List<MapleFoothold> allFootholds = new LinkedList<>();
        Point lBound = new Point();
        Point uBound = new Point();
        for (MapleData footRoot : mapData.getChildByPath("foothold")) {
            for (MapleData footCat : footRoot) {
                for (MapleData footHold : footCat) {
                    int x1 = MapleDataTool.getInt(footHold.getChildByPath("x1"));
                    int y1 = MapleDataTool.getInt(footHold.getChildByPath("y1"));
                    int x2 = MapleDataTool.getInt(footHold.getChildByPath("x2"));
                    int y2 = MapleDataTool.getInt(footHold.getChildByPath("y2"));
                    MapleFoothold fh = new MapleFoothold(new Point(x1, y1), new Point(x2, y2), Integer.parseInt(footHold.getName()));
                    fh.setPrev(MapleDataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext(MapleDataTool.getInt(footHold.getChildByPath("next")));
                    if (fh.getX1() < lBound.x) {
                        lBound.x = fh.getX1();
                    }
                    if (fh.getX2() > uBound.x) {
                        uBound.x = fh.getX2();
                    }
                    if (fh.getY1() < lBound.y) {
                        lBound.y = fh.getY1();
                    }
                    if (fh.getY2() > uBound.y) {
                        uBound.y = fh.getY2();
                    }
                    allFootholds.add(fh);
                }
            }
        }
        MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
        for (MapleFoothold fh : allFootholds) {
            fTree.insert(fh);
        }
        map.setFootholds(fTree);
        if (mapData.getChildByPath("area") != null) {
            for (MapleData area : mapData.getChildByPath("area")) {
                int x1 = MapleDataTool.getInt(area.getChildByPath("x1"));
                int y1 = MapleDataTool.getInt(area.getChildByPath("y1"));
                int x2 = MapleDataTool.getInt(area.getChildByPath("x2"));
                int y2 = MapleDataTool.getInt(area.getChildByPath("y2"));
                map.addMapleArea(new Rectangle(x1, y1, (x2 - x1), (y2 - y1)));
            }
        }
    }

    private void obtainSpawns() {
        try (Connection con = Server.getWorld(map.getWorld()).getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?")) {
            ps.setInt(1, map.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("idd");
                    if (GameConstants.isMapleTV(id)) {
                        continue;
                    }
                    int f = rs.getInt("f");
                    boolean hide = rs.getInt("hidden") == 1;
                    String type = rs.getString("type");
                    int fh = rs.getInt("fh");
                    int cy = rs.getInt("cy");
                    int rx0 = rs.getInt("rx0");
                    int rx1 = rs.getInt("rx1");
                    int x = rs.getInt("x");
                    int mobTime = rs.getInt("mobtime");

                    AbstractLoadedMapleLife life = MapleLifeFactory.getLife(id, type);
                    if (life == null) {
                        LOGGER.warn("No information for maple life {} type '{}'", id, type);
                        continue;
                    }
                    life.setCy(cy);
                    life.setF(f);
                    life.setFh(fh);
                    life.setRx0(rx0);
                    life.setRx1(rx1);
                    life.setPosition(new Point(x, cy));
                    life.setHide(hide);

                    if (type.equals("n") && NPCs) {
                        ((MapleNPC) life).setScript(rs.getString("script"));
                        map.addMapObject(life);
                    } else if (type.equals("m")) {
                        MapleMonster monster = ((MapleMonster) life);
                        if (!monsters) {
                            // always add spawn points
                            map.addMonsterSpawnPoint(new SpawnPoint(map, monster, !monster.isMobile(), map.getMonsterRate(), -1));
                        } else {
                            map.addMonsterSpawn((MapleMonster) life, mobTime, -1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to load SQL spawn information for map {}: {}", map.getId(), e.getMessage());
        }

        for (MapleData life : mapData.getChildByPath("life")) {
            String id = MapleDataTool.getString(life.getChildByPath("id"));
            String type = MapleDataTool.getString(life.getChildByPath("type"));
            if (id.equals("9001105")) {
                id = "9001108";//soz
            }
            AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type);
            if (myLife != null) {
                if (GameConstants.isMapleTV(myLife.getId())) {
                    continue;
                }

                myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
                MapleData dF = life.getChildByPath("f");
                if (dF != null) {
                    myLife.setF(MapleDataTool.getInt(dF));
                }
                myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
                myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
                myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
                int x = MapleDataTool.getInt(life.getChildByPath("x"));
                int y = MapleDataTool.getInt(life.getChildByPath("y"));
                myLife.setPosition(new Point(x, y));
                int hide = MapleDataTool.getInt("hide", life, 0);
                if (hide == 1) {
                    myLife.setHide(true);
                }
            }
            if (myLife != null) {
                if (myLife instanceof MapleMonster) {
                    MapleMonster monster = (MapleMonster) myLife;
                    int mobTime = MapleDataTool.getInt("mobTime", life, 0);
                    int team = MapleDataTool.getInt("team", life, -1);
                    if (monsters && mobTime == -1) { //does not respawn, force spawn once
                        map.spawnMonster(monster);
                    } else {
                        if (!monsters) {
                            SpawnPoint spawnPoint = new SpawnPoint(map, monster, !monster.isMobile(), mobTime, team);
                            map.addMonsterSpawnPoint(spawnPoint);
                        } else {
                            map.addMonsterSpawn(monster, mobTime, team);
                        }
                    }
                } else {
                    if (!NPCs && myLife.getType() == MapleMapObjectType.NPC) {
                        continue;
                    } else if (!reactors && myLife.getType() == MapleMapObjectType.REACTOR) {
                        continue;
                    }
                    map.addMapObject(myLife);
                }
            } else {
                LOGGER.warn("Unable to load life {} type '{}' in map {}", id, type, map.getId());
            }
        }
    }

    private void obtainReactors() {
        if (mapData.getChildByPath("reactor") != null) {
            for (MapleData rData : mapData.getChildByPath("reactor")) {
                String id = MapleDataTool.getString(rData.getChildByPath("id"));
                if (id != null) {
                    MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
                    Point point = new Point(MapleDataTool.getInt(rData.getChildByPath("x")), MapleDataTool.getInt(rData.getChildByPath("y")));
                    reactor.setPosition(point);
                    reactor.setDelay(MapleDataTool.getInt(rData.getChildByPath("reactorTime")) * 1000);
                    reactor.setState((byte) 0);
                    reactor.setName(MapleDataTool.getString(rData.getChildByPath("name"), ""));
                    map.spawnReactor(reactor);
                }
            }
        }
    }

    public MapleMap build() {
        mapData = source.getData(getMapName(map.getId()));
        if (mapData == null) {
            return null;
        }
        obtainInformoation();
        // @formatter:off
        if (portals) obtainPortals();
        if (footholds) obtainFootholds();
        obtainSpawns();
        if (reactors) obtainReactors();
        // @formatter:on
        return map;
    }

    private static String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        int area = mapid / 100000000;
        return "Map/Map" + area + "/" + mapName + ".img";
    }

    private static String getNodePath(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if (mapid < 200000000) {
            builder.append("victoria");
        } else if (mapid < 300000000) {
            builder.append("ossyria");
        } else if (mapid >= 540000000 && mapid < 551030200) {
            builder.append("singapore");
        } else if (mapid >= 600000000 && mapid < 620000000) {
            builder.append("MasteriaGL");
        } else if (mapid >= 670000000 && mapid < 682000000) {
            builder.append("weddingGL");
        } else if (mapid >= 682000000 && mapid < 683000000) {
            builder.append("HalloweenGL");
        } else if (mapid >= 800000000 && mapid < 900000000) {
            builder.append("jp");
        } else {
            builder.append("etc");
        }
        builder.append("/").append(mapid);
        return builder.toString();
    }
}