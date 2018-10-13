/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.handlers.login;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CheckCharNameHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        String username = slea.readMapleAsciiString();
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement("select * from ign_reserves where reserve = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (username.equalsIgnoreCase(rs.getString("reserve"))
                            && !rs.getString("username").equalsIgnoreCase(client.getAccountName())) {
                        client.announce(MaplePacketCreator.charNameResponse(username, true));
                        client.announce(MaplePacketCreator.serverNotice(0, "This ign is reserved for another user"));
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        client.announce(MaplePacketCreator.charNameResponse(username, !MapleCharacter.canCreateChar(username)));
    }
}
