package com.lucianms.server.maps;

import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MaplePortal;

public class MapMonitor {

    private Task monitorTask;

    public MapMonitor(final MapleMap map, String portalName) {
        MaplePortal portal = map.getPortal(portalName);

        monitorTask = TaskExecutor.createRepeatingTask(new Runnable() {
            @Override
            public void run() {
                if (map.getCharacters().isEmpty()) {
                    monitorTask.cancel();
                    monitorTask = null;

                    map.killAllMonsters();
                    map.clearDrops();
                    if (portal != null) {
                        portal.setPortalStatus(MaplePortal.OPEN);
                    }
                    map.resetReactors();
                }
            }
        }, 5000);
    }

}
