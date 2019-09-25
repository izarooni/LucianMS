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
package com.lucianms.helpers;

import com.lucianms.client.MapleJob;
import com.lucianms.server.Server;

import java.sql.*;

/**
 * @author Matze
 * @author Quit
 */
public class RankingWorker implements Runnable {

    public static final long UpdateInterval = 1000 * 60 * 60;
    private long lastUpdate = System.currentTimeMillis();

    public void run() {
        updateRanking(null);
        for (int i = 0; i < 3; i += 2) {
            for (int j = 1; j < 6; j++) {
                updateRanking(MapleJob.getById(i * 500 + 100 * j));
            }
        }
        lastUpdate = System.currentTimeMillis();
    }

    private void updateRanking(MapleJob job) {
        String sqlCharSelect = "SELECT c.id, " + (job != null ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") + ", a.last_login AS lastlogin, a.loggedin FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 ";
        if (job != null) {
            sqlCharSelect += "AND c.job DIV 100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.level DESC , c.exp DESC , c.fame DESC , c.meso DESC";
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement charSelect = con.prepareStatement(sqlCharSelect)) {
                if (job != null) {
                    charSelect.setInt(1, job.getId() / 100);
                }
                try (ResultSet rs = charSelect.executeQuery()) {
                    String query = (job != null) ? "jobRank = ?, jobRankMove = ?" : "`rank` = ?, rankMove = ?";
                    query = "update characters set " + query + " where id = ?";
                    try (PreparedStatement ps = con.prepareStatement(query)) {
                        for (int rank = 0; rs.next(); rank++) {
                            int rankMove = 0;
                            Timestamp lastlogin = rs.getTimestamp("lastlogin");
                            if (lastlogin.getTime() < lastUpdate || rs.getInt("loggedin") > 0) {
                                rankMove = rs.getInt((job != null ? "jobRankMove" : "rankMove"));
                            }
                            rankMove += rs.getInt((job != null ? "jobRank" : "rank")) - rank;
                            ps.setInt(1, rank);
                            ps.setInt(2, rankMove);
                            ps.setInt(3, rs.getInt("id"));
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
