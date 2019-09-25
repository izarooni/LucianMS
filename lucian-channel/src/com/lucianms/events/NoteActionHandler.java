package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class NoteActionHandler extends PacketEvent {

    private String username;
    private String content;
    private byte action;
    private int[] notes;

    @Override
    public void clean() {
        notes = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 0:
                username = reader.readMapleAsciiString();
                content = reader.readMapleAsciiString();
                break;
            case 1:
                byte count = reader.readByte();
                reader.readByte();
                reader.readByte();
                notes = new int[count];
                for (int i = 0; i < count; i++) {
                    notes[i] = reader.readInt();
                    reader.readByte();
                }
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (action == 0 && player.getCashShop().getAvailableNotes() > 0) {
            if (player.getCashShop().isOpened()) {
                getClient().announce(MaplePacketCreator.showCashInventory(getClient()));
            }
            player.sendNote(username, content, (byte) 1);
            player.getCashShop().decreaseNotes();
        } else if (action == 1) {
            int fame = 0;
            try (Connection con = getClient().getWorldServer().getConnection()) {
                for (int note : notes) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT `fame` FROM notes WHERE id=? AND deleted=0")) {
                        ps.setInt(1, note);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                fame += rs.getInt("fame");
                            }
                        }
                    }

                    try (PreparedStatement ps = con.prepareStatement("UPDATE notes SET `deleted` = 1 WHERE id = ?")) {
                        ps.setInt(1, note);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                getLogger().error("Unable to create database connection: {}", e.getMessage());
            }
            if (fame > 0) {
                player.gainFame(fame);
                getClient().announce(MaplePacketCreator.getShowFameGain(fame));
            }
        }
        return null;
    }
}
