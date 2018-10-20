package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.io.scripting.reactor.ReactorScriptManager;
import com.lucianms.server.maps.MapleReactor;

/**
 * @author izarooni
 */
public class PlayerReactorTouchEvent extends PacketEvent {

    private int objectId;
    private boolean touching;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectId = reader.readInt();
        touching = reader.readByte() == 0;
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleReactor reactor = player.getMap().getReactorByOid(objectId);
        if (reactor != null) {
            if (player.isDebug()) {
                player.sendMessage("Reactor {} ID {}, Name {}, State {}", touching, reactor.getId(), reactor.getName(), reactor.getState());
            }
            if (touching) {
                ReactorScriptManager.touch(getClient(), reactor);
            } else {
                ReactorScriptManager.untouch(getClient(), reactor);
            }
        }
        return null;
    }
}
