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
package server.maps;

import client.MapleCharacter;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.PortalFactory;
import server.life.*;
import tools.DatabaseConnection;
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
import java.util.concurrent.ConcurrentHashMap;

public class MapleMapFactory {

    private MapleDataProvider source;
    private MapleData nameData;
    private ConcurrentHashMap<Integer, MapleMap> maps = new ConcurrentHashMap<>();
    private int channel, world;

    private boolean skipMonsters = false;

    public MapleMapFactory(int world, int channel) {
        this.source = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz"));
        this.nameData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")).getData("Map.img");
        this.world = world;
        this.channel = channel;
    }

    public MapleMapFactory skipMonsters(boolean skipMonsters) {
        // as long as the value is not null it will skip monsters
        this.skipMonsters = skipMonsters;
        return this;
    }

    public MapleMap getMap(final int mapid) {
        try {
            if (!maps.containsKey(mapid)) {
                String mapName = getMapName(mapid);
                MapleData mapData = source.getData(mapName);
                if (mapData == null) {
                    return null;
                }
                String link = MapleDataTool.getString(mapData.getChildByPath("info/link"), "");
                if (!link.equals("")) { //nexon made hundreds of dojo maps so to reduce the size they added links.
                    mapName = getMapName(Integer.parseInt(link));
                    mapData = source.getData(mapName);
                }
                float monsterRate = 0;
                MapleData mobRate = mapData.getChildByPath("info/mobRate");
                if (mobRate != null) {
                    monsterRate = (Float) mobRate.getData();
                }
                MapleMap map = new MapleMap(mapid, world, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);

                String onFirstEnter = MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), String.valueOf(mapid));
                map.setOnFirstUserEnter(onFirstEnter.equals("") ? String.valueOf(mapid) : onFirstEnter);

                String onEnter = MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), String.valueOf(mapid));
                map.setOnUserEnter(onEnter.equals("") ? String.valueOf(mapid) : onEnter);

                map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
                map.setMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 5000));
                PortalFactory portalFactory = new PortalFactory();
                for (MapleData portal : mapData.getChildByPath("portal")) {
                    map.addPortal(portalFactory.makePortal(MapleDataTool.getInt(portal.getChildByPath("pt")), portal));
                }
                MapleData timeMob = mapData.getChildByPath("info/timeMob");
                if (timeMob != null) {
                    map.timeMob(MapleDataTool.getInt(timeMob.getChildByPath("id")), MapleDataTool.getString(timeMob.getChildByPath("message")));
                }

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

                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?");
                    ps.setInt(1, mapid);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int id = rs.getInt("idd");
                        int f = rs.getInt("f");
                        boolean hide = rs.getInt("hidden") == 1;
                        String type = rs.getString("type");
                        int fh = rs.getInt("fh");
                        int cy = rs.getInt("cy");
                        int rx0 = rs.getInt("rx0");
                        int rx1 = rs.getInt("rx1");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int mobTime = rs.getInt("mobtime");

                        AbstractLoadedMapleLife myLife = giveLife(id, f, hide, fh, cy, rx0, rx1, x, y, type);

                        if (type.equals("n")) {
                            ((MapleNPC) myLife).setScript(rs.getString("script"));
                            map.addMapObject(myLife);
                        } else if (type.equals("m")) {
                            MapleMonster monster = ((MapleMonster) myLife);
                            if (skipMonsters) {
                                map.addMonsterSpawnPoint(new SpawnPoint(monster, monster.getPosition(), !monster.isMobile(), 0, 5000, -1));
                            } else {
                                map.addMonsterSpawn((MapleMonster) myLife, mobTime, -1);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM playernpcs WHERE map = ?")) {
                        ps.setInt(1, mapid);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                map.addMapObject(new PlayerNPC(rs));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (MapleData life : mapData.getChildByPath("life")) {
                    String id = MapleDataTool.getString(life.getChildByPath("id"));
                    String type = MapleDataTool.getString(life.getChildByPath("type"));
                    if (id.equals("9001105")) {
                        id = "9001108";//soz
                    }
                    AbstractLoadedMapleLife myLife = loadLife(life, id, type);
                    if (myLife instanceof MapleMonster) {
                        MapleMonster monster = (MapleMonster) myLife;
                        int mobTime = MapleDataTool.getInt("mobTime", life, 0);
                        int team = MapleDataTool.getInt("team", life, -1);
                        if (!skipMonsters && mobTime == -1) { //does not respawn, force spawn once
                            map.spawnMonster(monster);
                        } else {
                            if (skipMonsters) {
                                SpawnPoint spawnPoint = new SpawnPoint(monster, map.calcPointBelow(monster.getPosition()), !monster.isMobile(), mobTime, 5000, team);
                                map.addMonsterSpawnPoint(spawnPoint);
                            } else {
                                map.addMonsterSpawn(monster, mobTime, team);
                            }
                        }
                    } else {
                        map.addMapObject(myLife);
                    }
                }
                if (mapData.getChildByPath("reactor") != null) {
                    for (MapleData reactor : mapData.getChildByPath("reactor")) {
                        String id = MapleDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            MapleReactor newReactor = loadReactor(reactor, id);
                            map.spawnReactor(newReactor);
                        }
                    }
                }
                try {
                    map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), ""));
                    map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), ""));
                } catch (Exception e) {
                    map.setMapName("");
                    map.setStreetName("");
                }

                map.setClock(mapData.getChildByPath("clock") != null);
                map.setEverlast(mapData.getChildByPath("everlast") != null);
                map.setTown(MapleDataTool.getInt("info/town", mapData, 0) == 1);
                map.setHPDec(MapleDataTool.getIntConvert("info/decHP", mapData, 0));
                map.setHPDecProtect(MapleDataTool.getIntConvert("info/protectItem", mapData, 0));
                map.setForcedReturnMap(MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
                map.setBoat(mapData.getChildByPath("shipObj") != null);
                map.setTimeLimit(MapleDataTool.getIntConvert("timeLimit", mapData.getChildByPath("info"), -1));
                map.setFieldType(MapleDataTool.getIntConvert("info/fieldType", mapData, 0));
                map.setMobCapacity(MapleDataTool.getIntConvert("fixedMobCapacity", mapData.getChildByPath("info"), 500));//Is there a map that contains more than 500 mobs?

                HashMap<Integer, Integer> backTypes = new HashMap<>();
                try {
                    for (MapleData layer : mapData.getChildByPath("back")) { // yolo
                        int layerNum = Integer.parseInt(layer.getName());
                        int type = MapleDataTool.getInt(layer.getChildByPath("type"), 0);

                        backTypes.put(layerNum, type);
                    }
                } catch (Exception e) {
                    // swallow cause I'm cool
                }
                map.setBackgroundTypes(backTypes);

                maps.put(mapid, map);
                return map;
            }
            return maps.get(mapid);
        } finally {
            // these must be reset as they're only used once per invoke
            // to prevent fucking things up with maps that should be loaded normally
            skipMonsters = false;
        }
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    private AbstractLoadedMapleLife loadLife(MapleData life, String id, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type);
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
        return myLife;
    }

    private MapleReactor loadReactor(MapleData reactor, String id) {
        MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
        int x = MapleDataTool.getInt(reactor.getChildByPath("x"));
        int y = MapleDataTool.getInt(reactor.getChildByPath("y"));
        myReactor.setPosition(new Point(x, y));
        myReactor.setDelay(MapleDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setState((byte) 0);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("name"), ""));
        return myReactor;
    }

    private String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        int area = mapid / 100000000;
        return "Map/Map" + area + "/" + mapName + ".img";
    }

    private String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if (mapid >= 100000000 && mapid < 200000000) {
            builder.append("victoria");
        } else if (mapid >= 200000000 && mapid < 300000000) {
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


    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setWorld(int world) {
        this.channel = world;
    }

    public ArrayList<MapleMap> getMaps() {
        return new ArrayList<>(maps.values());
    }

    public void reloadField(int fieldId) {
        MapleMap fOld, fNew;
        if ((fOld = maps.get(fieldId)) != null) {
            maps.remove(fieldId);
            fNew = getMap(fieldId);
            for (MapleMapObject object : fOld.getMapObjects()) {
                if (object instanceof MapleCharacter) {
                    MapleCharacter player = (MapleCharacter) object;
                    fOld.removeMapObject(player);
                    player.changeMap(fNew);
                }
            }
            maps.put(fieldId, fNew);
        }
    }

    private AbstractLoadedMapleLife giveLife(int id, int f, boolean hide, int fh, int cy, int rx0, int rx1, int x, int y, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(id, type);
        myLife.setCy(cy);
        myLife.setF(f);
        myLife.setFh(fh);
        myLife.setRx0(rx0);
        myLife.setRx1(rx1);
        myLife.setPosition(new Point(x, y));
        myLife.setHide(hide);
        return myLife;
    }
}
