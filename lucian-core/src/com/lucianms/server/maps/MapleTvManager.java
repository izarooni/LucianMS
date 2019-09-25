package com.lucianms.server.maps;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class MapleTvManager {


    private String[] message;
    private MapleCharacter player;
    private MapleCharacter partner;
    private int type;

    public MapleTvManager(MapleCharacter player, MapleCharacter partner, String[] message, int type) {
        this.player = player;
        this.partner = partner;
        this.message = message;
        this.type = type;
    }

    public void sendMessage(MapleWorld world) {
        world.sendPacket(MaplePacketCreator.getMapleTvSendMessage());
        world.sendPacket(MaplePacketCreator.getMapleTvSetMessage(player, message, type <= 2 ? type : type - 3, partner));
        int delay;
        if (type == 4) {
            delay = 30000;
        } else if (type == 5) {
            delay = 60000;
        } else {
            delay = 15000;
        }
        TaskExecutor.createTask(() -> clearMessage(world), delay);
    }

    private void clearMessage(MapleWorld world) {
        world.sendPacket(MaplePacketCreator.getMapleTvClearMessage());
    }
}
