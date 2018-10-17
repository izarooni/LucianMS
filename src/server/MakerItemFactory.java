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
package server;

import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jay Estrella
 */
public class MakerItemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MakerItemFactory.class);
    private static Map<Integer, MakerItemCreateEntry> createCache = new HashMap<Integer, MakerItemCreateEntry>();

    public static MakerItemCreateEntry getItemCreateEntry(int toCreate) {
        if (createCache.get(toCreate) != null) {
            return createCache.get(toCreate);
        } else {
            try (Connection con = Server.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT req_level, req_maker_level, req_meso, quantity FROM makercreatedata WHERE itemid = ?")) {
                    ps.setInt(1, toCreate);
                    try (ResultSet rs = ps.executeQuery()) {
                        int reqLevel = 0;
                        int reqMakerLevel = 0;
                        int cost = 0;
                        int toGive = 0;
                        if (rs.next()) {
                            reqLevel = rs.getInt("req_level");
                            reqMakerLevel = rs.getInt("req_maker_level");
                            cost = rs.getInt("req_meso");
                            toGive = rs.getInt("quantity");
                        }
                        MakerItemCreateEntry ret = new MakerItemCreateEntry(cost, reqLevel, reqMakerLevel, toGive);

                        try (PreparedStatement pps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ?")) {
                            pps.setInt(1, toCreate);
                            try (ResultSet rrs = pps.executeQuery()) {
                                while (rrs.next()) {
                                    ret.addReqItem(rrs.getInt("req_item"), rrs.getInt("count"));
                                }
                            }
                        }
                        createCache.put(toCreate, ret);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Unable to create database connection: {}", e.getMessage());
            }
        }
        return createCache.get(toCreate);
    }

    public static class MakerItemCreateEntry {
        private int reqLevel, reqMakerLevel;
        private int cost;
        private List<Pair<Integer, Integer>> reqItems = new ArrayList<Pair<Integer, Integer>>(); // itemId / amount
        private int toGive;

        private MakerItemCreateEntry(int cost, int reqLevel, int reqMakerLevel, int toGive) {
            this.cost = cost;
            this.reqLevel = reqLevel;
            this.reqMakerLevel = reqMakerLevel;
            this.toGive = toGive;
        }

        public int getRewardAmount() {
            return toGive;
        }

        public List<Pair<Integer, Integer>> getReqItems() {
            return reqItems;
        }

        public int getReqLevel() {
            return reqLevel;
        }

        public int getReqSkillLevel() {
            return reqMakerLevel;
        }

        public int getCost() {
            return cost;
        }

        protected void addReqItem(int itemId, int amount) {
            reqItems.add(new Pair<Integer, Integer>(itemId, amount));
        }
    }
}
