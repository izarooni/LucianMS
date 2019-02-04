package com.lucianms.server.handlers.login;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author izarooni
 */
public class ViewCharHandler extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT world, id FROM characters WHERE accountid = ?")) {
            ps.setInt(1, getClient().getAccID());

            int charsNum = 0;
            TreeMap<Byte, ArrayList<MapleCharacter>> allPlayers = new TreeMap<>();
            try {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ArrayList<MapleCharacter> world = allPlayers.computeIfAbsent(rs.getByte("world"), b -> new ArrayList<>());
                        world.add(MapleCharacter.loadCharFromDB(con, rs.getInt("id"), getClient(), false));
                        charsNum++;
                    }
                }
                int unk = charsNum + 3 - charsNum % 3;
                getClient().announce(MaplePacketCreator.showAllCharacter(charsNum, unk));

                for (Map.Entry<Byte, ArrayList<MapleCharacter>> entry : allPlayers.entrySet()) {
                    getClient().announce(MaplePacketCreator.showAllCharacterInfo(entry.getKey(), entry.getValue()));
                }
            } finally {
                allPlayers.clear();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
