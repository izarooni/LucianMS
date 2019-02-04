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
package com.lucianms.client.inventory;

import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.movement.AbsoluteLifeMovement;
import com.lucianms.server.movement.LifeMovement;
import com.lucianms.server.movement.LifeMovementFragment;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Matze
 */
public class MaplePet extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaplePet.class);

    private String name;
    private int uniqueid;
    private int closeness = 0;
    private byte level = 1;
    private int fullness = 100;
    private int Fh = 0;
    private Point pos = new Point();
    private int stance;
    private boolean summoned = false;

    private MaplePet(int id, short position, int uniqueid) {
        super(id, position, (short) 1);
        this.uniqueid = uniqueid;
    }

    public static MaplePet loadFromDb(int itemid, short position, int petid) {
        MaplePet ret = new MaplePet(itemid, position, petid);
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name, level, closeness, fullness, summoned FROM pets WHERE petid = ?")) {
            ps.setInt(1, petid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret.setName(rs.getString("name"));
                    ret.setCloseness(Math.min(rs.getInt("closeness"), 30000));
                    ret.setLevel((byte) Math.min(rs.getByte("level"), 30));
                    ret.setFullness(Math.min(rs.getInt("fullness"), 100));
                    ret.setSummoned(rs.getInt("summoned") == 1);
                    return ret;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to load pet {} from database", itemid, e);
        }
        return null;
    }

    public void saveToDb(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, summoned = ? WHERE petid = ?")) {
            ps.setString(1, getName());
            ps.setInt(2, getLevel());
            ps.setInt(3, getCloseness());
            ps.setInt(4, getFullness());
            ps.setInt(5, isSummoned() ? 1 : 0);
            ps.setInt(6, getUniqueId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Unable to save pet {} to database", getItemId(), e);
        }
    }

    public static int createPet(int itemid) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO pets (name, level, closeness, fullness, summoned) VALUES (?, 1, 0, 100, 0)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            String petName = MapleItemInformationProvider.getInstance().getName(itemid);
            if (petName == null) {
                petName = "MISSINGNO";
            }
            ps.setString(1, petName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int ret = -1;
                if (rs.next()) {
                    ret = rs.getInt(1);
                }
                return ret;
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to create pet {}", itemid, e);
        }
        return -1;
    }

    public static int createPet(int itemid, byte level, int closeness, int fullness) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO pets (name, level, closeness, fullness, summoned) VALUES (?, ?, ?, ?, 0)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, MapleItemInformationProvider.getInstance().getName(itemid));
            ps.setByte(2, level);
            ps.setInt(3, closeness);
            ps.setInt(4, fullness);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int ret = -1;
                if (rs.next()) {
                    ret = rs.getInt(1);
                }
                return ret;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int id) {
        this.uniqueid = id;
    }

    public int getCloseness() {
        return closeness;
    }

    public void setCloseness(int closeness) {
        this.closeness = closeness;
    }

    public void gainCloseness(int x) {
        this.closeness += x;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public int getFullness() {
        return fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = fullness;
    }

    public int getFh() {
        return Fh;
    }

    public void setFh(int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public boolean isSummoned() {
        return summoned;
    }

    public void setSummoned(boolean yes) {
        this.summoned = yes;
    }

    public boolean canConsume(int itemId) {
        for (int petId : MapleItemInformationProvider.getInstance().petsCanConsume(itemId)) {
            if (petId == this.getItemId()) {
                return true;
            }
        }
        return false;
    }

    public void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    this.setPos(move.getPosition());
                }
                this.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}