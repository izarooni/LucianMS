package com.lucianms.features.controllers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.Server;
import com.lucianms.server.maps.MapleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JumpQuestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JumpQuestController.class);

    private MapleCharacter player;
    private int map, id;
    private long timeStarted;
    private int mapComingFrom;

    public JumpQuestController(MapleCharacter player, int id, int map, int previousMap) {
        this.player = player;
        this.map = map;
        this.id = id;
        this.mapComingFrom = previousMap;
    }

    public void start() {
        FieldBuilder builder = new FieldBuilder(player.getWorld(), player.getClient().getChannel(), map);
        MapleMap map = builder.loadAll().build();

        map.getPortals().forEach((portal) -> portal.setPortalStatus(true)); // disabling portals.
        player.changeMap(map);
        player.dropMessage(6, "Good luck, you'll need it.");

        timeStarted = System.currentTimeMillis();

    }

    public int getHighscore() {
        int highscore = 0;
        try (Connection con = player.getClient().getWorldServer().getConnection();
             PreparedStatement stmnt = con.prepareStatement("SELECT time FROM jq_scores WHERE charid = ? AND id = ?")) {
            stmnt.setInt(1, player.getId());
            stmnt.setInt(2, id);
            stmnt.execute();
            try (ResultSet rs = stmnt.getResultSet()) {
                while (rs.next()) {
                    highscore = rs.getInt("time");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highscore;
    }


    public static String getTop(int id) {
        StringBuilder sb = new StringBuilder();
        try (Connection con = Server.getConnection();
             PreparedStatement stmnt = con.prepareStatement("SELECT * FROM jq_scores WHERE id = ? ORDER BY time DESC LIMIT 50")) {
            stmnt.setInt(1, id);

            if (stmnt.execute()) {
                try (ResultSet rs = stmnt.getResultSet()) {
                    int i = 0;
                    while (rs.next()) {
                        String user = MapleCharacter.getNameById(rs.getInt("charid"));
                        if (user != null) {
                            i++;
                            sb.append("#k")
                                    .append(i <= 3 ? "#b" : "")
                                    .append(i > 3 && i <= 5 ? "#g" : "")
                                    .append(i).append(". ")
                                    .append(user)
                                    .append(" with a time of ")
                                    .append(rs.getInt("time"))
                                    .append(" seconds\r\n");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public void end() {
        int score = getHighscore();
        try (Connection c = player.getClient().getWorldServer().getConnection();
             PreparedStatement stmnt = c.prepareStatement("INSERT INTO jq_scores (id, charid, time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE time = ?")) {
            int time = (int) ((System.currentTimeMillis() - timeStarted) / 1000);
            time = (score >= time ? time : score);

            stmnt.setInt(1, id);
            stmnt.setInt(2, player.getId());
            stmnt.setInt(3, time); // time in seconds
            stmnt.setInt(4, time); // time in seconds

            stmnt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            player.changeMap(getReturnMap());

        }

    }

    public int getJQMap() {
        return this.map;
    }

    public int getReturnMap() {
        return this.mapComingFrom;
    }

}
