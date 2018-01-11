package net.server.channel.handlers;

import client.MapleCharacter;
import net.PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class ReactorHitHandler extends PacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorHitHandler.class);

    private int objectId;
    private int position;
    private int skillId;

    private short stance;

    @Override
    public void exceptionCaught(Throwable t) {
        super.exceptionCaught(t);
        MapleReactor reactor = getClient().getPlayer().getMap().getReactorById(objectId);
        if (reactor != null) {
            LOGGER.warn("An occurred occurred within the reactor '{}'", reactor.getId());
        }
    }

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        objectId = slea.readInt();
        position = slea.readInt();
        stance = slea.readShort();
        slea.skip(4);
        skillId = slea.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleReactor reactor = player.getMap().getReactorByOid(objectId);
        if (player.isDebug()) {
            player.sendMessage("Reactor Hit ID: {}, Name: {}, State {}", reactor.getId(), reactor.getName(), reactor.getState());
        }
        if (reactor != null && reactor.isAlive()) {
            reactor.hitReactor(getClient(), position, stance, skillId);
        }
        return null;
    }
}
