package com.lucianms.server.maps.tasks;

import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleWorld;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class FieldUpdateTask implements Runnable {

    private static final long RespawnInterval = 10000;
    private MapleWorld world;

    public FieldUpdateTask(MapleWorld world) {
        this.world = world;
    }

    @Override
    public void run() {
        try {
            for (MapleChannel channel : world.getChannels()) {
                ArrayList<MapleMap> maps = new ArrayList<>(channel.getMaps());
                for (MapleMap map : maps) {
                    map.respawn();
                }
                maps.clear();
            }
        } finally {
            TaskExecutor.createTask(new FieldUpdateTask(world), RespawnInterval);
        }
    }
}
