package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;

/**
 * @author Moogra
 * @author izarooni
 */
public class PlayerFamilyUseEvent extends PacketEvent {

    private String username;
    private int action;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readInt();
        if (action == 0 || action == 1) {
            username = reader.readMapleAsciiString();
        }
    }

    @Override
    public Object onPacket() {
        if (!ServerConstants.USE_FAMILY_SYSTEM) {
            return null;
        }
        MapleCharacter player = getClient().getPlayer();
        int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
        if (action == 0 || action == 1) {
            MapleCharacter target = getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
            if (target == null) {
                return null;
            }
            if (action == 0) {
                player.changeMap(target.getMap(), target.getMap().getPortal(0));
            } else {
                target.changeMap(player.getMap(), player.getMap().getPortal(0));
            }
        } else {
            int erate = action == 3 ? 150 : (action == 4 || action == 6 || action == 8 || action == 10 ? 200 : 100);
            int drate = action == 2 ? 150 : (action == 4 || action == 5 || action == 7 || action == 9 ? 200 : 100);
            if (action <= 8) {
                getClient().announce(useRep(drate == 100 ? 2 : (erate == 100 ? 3 : 4), action, erate, drate, ((action > 5 || action == 4) ? 2 : 1) * 15 * 60 * 1000));
            }
        }
        player.getFamily().getMember(player.getId()).gainReputation(repCost[action]);
        return null;
    }

    private static byte[] useRep(int mode, int type, int erate, int drate, int time) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.FAMILY_RESULT.getValue());
        w.write(mode);
        w.writeInt(type);
        if (mode < 4) {
            w.writeInt(erate);
            w.writeInt(drate);
        }
        w.write(0);
        w.writeInt(time);
        return w.getPacket();
    }

    private static byte[] giveBuff() {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.GIVE_BUFF.getValue());
        w.writeInt(0);
        w.writeLong(0);
        return null;
    }
}
