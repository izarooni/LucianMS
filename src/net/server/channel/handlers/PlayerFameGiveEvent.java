package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleCharacter.FameStatus;
import client.MapleStat;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerFameGiveEvent extends PacketEvent {

    private int objectID;
    private byte mode;
    private byte fame;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectID = reader.readInt();
        mode = reader.readByte();
        fame = (byte) (2 * mode - 1);
        if (fame != 1 && fame != -1) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMapObject mapObject = player.getMap().getMapObject(objectID);
        if (!(mapObject instanceof MapleCharacter)) {
            return null;
        }
        MapleCharacter target = (MapleCharacter) mapObject;
        if (target.getId() == player.getId() || player.getLevel() < 15) {
            return null;
        }
        FameStatus status = player.canGiveFame(target);
        if (status == FameStatus.OK || player.isGM()) {
            if (Math.abs(target.getFame() + fame) < 30001) {
                target.addFame(fame);
                target.updateSingleStat(MapleStat.FAME, target.getFame());
            }
            if (!player.isGM()) {
                player.hasGivenFame(target);
            }
            getClient().announce(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
            target.getClient().announce(MaplePacketCreator.receiveFame(mode, player.getName()));
        } else {
            getClient().announce(MaplePacketCreator.giveFameErrorResponse(status == FameStatus.NOT_TODAY ? 3 : 4));
        }
        return null;
    }
}