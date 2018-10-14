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
package server.maps;

import client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XoticStory
 */
public class PlayerNPC extends AbstractMapleMapObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerNPC.class);

    private Map<Short, Integer> equips = new HashMap<>();
    private int npcId, face, hair;
    private byte skin;
    private String name = "";
    private String script = null;
    private int FH, RX0, RX1, CY;

    public PlayerNPC(ResultSet rs) throws SQLException {
        name = rs.getString("name");
        hair = rs.getInt("hair");
        face = rs.getInt("face");
        skin = rs.getByte("skin");

        FH = rs.getInt("foothold");
        CY = rs.getInt("cy");
        RX0 = rs.getInt("rx0");
        RX1 = rs.getInt("rx1");
        setPosition(new Point(rs.getInt("x"), CY));

        npcId = rs.getInt("scriptid");
        script = rs.getString("script");

        try (PreparedStatement ps = Database.getConnection().prepareStatement("select equippos, equipid from playernpcs_equip where npcid = ?")) {
            ps.setInt(1, rs.getInt("id"));
            try (ResultSet rs2 = ps.executeQuery()) {
                while (rs2.next()) {
                    equips.put(rs2.getShort("equippos"), rs2.getInt("equipid"));
                }
            }
        }
    }

    public Map<Short, Integer> getEquips() {
        return equips;
    }

    public int getId() {
        return npcId;
    }

    public int getFH() {
        return FH;
    }

    public int getRX0() {
        return RX0;
    }

    public int getRX1() {
        return RX1;
    }

    public int getCY() {
        return CY;
    }

    public byte getSkin() {
        return skin;
    }

    public String getName() {
        return name;
    }

    public int getFace() {
        return face;
    }

    public int getHair() {
        return hair;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removeNPC(getObjectId()));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER_NPC;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(MaplePacketCreator.spawnPlayerNPC(this));
        client.announce(MaplePacketCreator.getPlayerNPC(this));
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}