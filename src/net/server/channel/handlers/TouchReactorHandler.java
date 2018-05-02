package net.server.channel.handlers;

import client.MapleCharacter;
import net.PacketHandler;
import com.lucianms.scripting.reactor.ReactorScriptManager;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class TouchReactorHandler extends PacketHandler {

    private int objectId;
    private boolean touching;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        objectId = slea.readInt();
        touching = slea.readByte() == 0;
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
