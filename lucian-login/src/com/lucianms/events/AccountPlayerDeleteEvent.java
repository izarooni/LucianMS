package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class AccountPlayerDeleteEvent extends PacketEvent {

    private String PIC;
    private int playerID;

    @Override
    public void exceptionCaught(MaplePacketReader reader, Throwable t) {
        getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 9));
        super.exceptionCaught(reader, t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        PIC = reader.readMapleAsciiString();
        playerID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        if (getClient().checkPic(PIC)) {
            try (Connection con = Server.getConnection()) {
                MapleCharacter.deletePlayer(con, playerID);
                getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 0));
            } catch (SQLException e) {
                getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 9));
                getLogger().error("Failed to delete character {}", playerID, e);
            }
        } else {
            getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 20));
        }
        return null;
    }
}