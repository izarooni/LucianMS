package com.lucianms.features.auto;

import client.MapleCharacter;
import com.lucianms.scheduler.TaskExecutor;
import net.SendOpcode;
import net.server.channel.Channel;
import net.server.world.World;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MonsterListener;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class AEmergencyTrial extends GAutoEvent {

    private static final int BossMapID = 240020600;
    public static final int EndMapID = 240040611;
    private static final int NpcID = 9010022;
    private static final int BossID = 100100;

    private HashMap<Integer, Integer> spawnLocations = new HashMap<>();
    private HashMap<Integer, Integer> returnLocations = new HashMap<>();

    private long startTimestamp;

    public static byte[] createNpc(MapleNPC life, Point location) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(24);
        mplew.writeShort(SendOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(location.x);
        mplew.writeShort(location.y);
        mplew.write(0);
        mplew.writeShort(0);
        mplew.writeShort(location.x - 50);
        mplew.writeShort(location.x + 50);
        mplew.write(1);
        return mplew.getPacket();
    }

    public AEmergencyTrial(World world, boolean nMapInstances) {
        super(world, nMapInstances);
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void start() {
        for (Channel channel : getWorld().getChannels()) {
            channel.removeMap(BossMapID);
            for (MapleCharacter player : channel.getPlayerStorage().getAllCharacters()) {
                MapleMap map = player.getMap();
                if (!map.getMonsterSpawnPoints().isEmpty()) {
                    Integer objectID = spawnLocations.get(map.getId());
                    MapleNPC npc;
                    if (objectID == null) {
                        npc = MapleLifeFactory.getNPC(NpcID);
                        npc.setScript("f_emergency_trial");
                        map.addMapObject(npc);
                    } else {
                        npc = (MapleNPC) map.getMapObject(objectID);
                    }
                    spawnLocations.putIfAbsent(map.getId(), npc.getObjectId());
                    player.announce(createNpc(npc, player.getPosition()));
                    player.sendMessage(5, "A mysterious portal has appeared in the map");
                }
            }
        }

        startTimestamp = System.currentTimeMillis();
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                despawnDoors();
                summonBoss();
            }
        }, 1000 * 30);
    }

    @Override
    public void stop() {
        despawnDoors();
        returnPlayers();
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        returnLocations.put(player.getId(), player.getMapId());
        player.changeMap(BossMapID);
        long left = ((startTimestamp + (1000 * 30)) - System.currentTimeMillis());
        if (left > 0) {
            player.announce(MaplePacketCreator.getClock((int) (left / 1000)));
        }
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.changeMap(EndMapID);
    }

    private void returnPlayers() {
        for (Channel channel : getWorld().getChannels()) {
            MapleMap map = channel.getMap(BossMapID);
            for (Integer playerID : new ArrayList<>(returnLocations.values())) {
                MapleCharacter player = map.getCharacterById(playerID);
                if (player != null) {
                    playerUnregistered(player);
                }
            }
            channel.removeMap(BossMapID);
        }
    }

    private void summonBoss() {
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                returnPlayers();
            }
        }, 1000 * 120);

        for (Channel channel : getWorld().getChannels()) {
            MapleMonster monster = MapleLifeFactory.getMonster(BossID);
            if (monster != null) {
                monster.addListener(new MonsterListener() {
                    @Override
                    public void monsterKilled(int aniTime) {
                        monster.getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/5"));
                        monster.getMap().broadcastMessage(MaplePacketCreator.playSound("PSO2/Completed"));
                        TaskExecutor.createTask(() -> monster.getMap().warpEveryone(EndMapID), 2500);
                    }
                });
                MapleMap map = channel.getMap(BossMapID);
                map.broadcastMessage(MaplePacketCreator.getClock(120));
                map.spawnMonsterOnGroudBelow(monster, new Point(-36, 452));
            } else {
                stop();
            }
        }
    }

    private void despawnDoors() {
        for (Map.Entry<Integer, Integer> entry : spawnLocations.entrySet()) {
            for (Channel channel : getWorld().getChannels()) {
                MapleMap map = channel.getMap(entry.getKey());
                MapleMapObject object = map.removeMapObject(entry.getValue());
                if (object != null) {
                    map.broadcastMessage(MaplePacketCreator.removeNPC(object.getObjectId()));
                }
            }
        }
    }

    public Integer popLocation(int playerID) {
        return returnLocations.remove(playerID);
    }
}
