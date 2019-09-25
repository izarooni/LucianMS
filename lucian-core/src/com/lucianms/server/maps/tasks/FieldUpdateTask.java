package com.lucianms.server.maps.tasks;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.meta.Occupation;
import com.lucianms.constants.ServerConstants;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author izarooni
 */
public class FieldUpdateTask implements Runnable {

    private final UpdateRecord RespawnInterval = new UpdateRecord(MapleMap.RESPAWN_INTERVAL);
    private final UpdateRecord CharacterInterval = new UpdateRecord(10000);
    private MapleWorld world;

    public FieldUpdateTask(MapleWorld world) {
        this.world = world;
    }

    @Override
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public void run() {
        try {
            boolean mapRecord = RespawnInterval.record();
            boolean userRecord = CharacterInterval.record();
            for (MapleChannel channel : world.getChannels()) {
                ArrayList<MapleMap> maps = new ArrayList<>(channel.getMaps());
                for (MapleMap map : maps) {
                    if (mapRecord) {
                        map.respawn();
                    }
                    if (!map.getEverlast()) {
                        ArrayList<MapleMapItem> list = map.getMapObjects(MapleMapItem.class);
                        Iterator<MapleMapItem> drops = list.iterator();
                        while (drops.hasNext()) {
                            MapleMapItem drop = drops.next();
                            if (System.currentTimeMillis() - drop.getDropTime() >= 180000) {
                                map.removeMapObject(drop);
                                drop.setPickedUp(true);
                                map.sendPacket(MaplePacketCreator.removeItemFromMap(drop.getObjectId(), 0, 0));
                            }
                        }
                        list.clear();
                    }
                    if (userRecord) {
                        Iterator<MapleCharacter> iterator = map.getCharacters().iterator();
                        while (iterator.hasNext()) {
                            MapleCharacter player = iterator.next();

                            player.checkExpirations();
                            if (map.getHPDec() > 0) {
                                if (player.getInventory(MapleInventoryType.EQUIPPED).findById(map.getHPDecProtect()) != null) {
                                    player.addHP(-map.getHPDec());
                                }
                            }

                            Occupation occupation = player.getOccupation();
                            /*
                            if (occupation != null) {

                                if (occupation.getType() == Occupation.Type.Troll &&
                                        player.getMapId() == ServerConstants.HOME_MAP) {
                                    occupation.gainExperience(10);
                                }
                            }
                             */
                        }
                    }
                }
                maps.clear();
            }
        } finally {
            TaskExecutor.createTask(new FieldUpdateTask(world), 5000);
        }
    }

    private class UpdateRecord {
        private final long updateInterval;
        private long lastUpdate;

        public UpdateRecord(long updateInterval) {
            this.updateInterval = updateInterval;
        }

        public boolean record() {
            long now = System.currentTimeMillis();
            if (now - lastUpdate >= updateInterval) {
                lastUpdate = now;
                return true;
            }
            return false;
        }
    }
}
