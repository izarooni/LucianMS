package com.lucianms.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;
import tools.Randomizer;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author izarooni
 */
public class JailManager {

    public static class JailLog {
        public final long timestamp;
        public final int playerId;
        public final int accuser;
        public final String reason;

        private JailLog(int playerId, int accuser, String reason, long timestamp) {
            this.playerId = playerId;
            this.accuser = accuser;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JailManager.class);
    private static final int[] Fields = new int[]{80, 81};

    private JailManager() {
    }

    public static int getRandomField() {
        return Fields[Randomizer.nextInt(Fields.length)];
    }

    public static boolean isJailField(int fieldId) {
        return Arrays.binarySearch(Fields, fieldId) >= 0;
    }

    public static boolean isJailed(int playerId) {
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("select * from jails where playerid = ?")) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to check if player '{}' is jailed", playerId, e);
        }
        return false;
    }

    public static void insertJail(int target, int accuser, String reason) {
        if (reason == null || reason.isEmpty()) {
            throw new InvalidParameterException("Reason must be specified");
        }
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("insert into jails (playerid, reason, accuser) values (?, ?, ?)")) {
            ps.setInt(1, target);
            ps.setString(2, reason);
            ps.setInt(3, accuser);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Unable to insert jail data for player '{}' reason '{}'", target, accuser, e);
        }
    }

    public static void removeJail(int playerId) {
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("delete from jails where playerid = ?")) {
            ps.setInt(1, playerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Unable to remove jail data for player '{}'", playerId, e);
        }
    }

    public static ArrayList<JailLog> retrieveLogs() {
        ArrayList<JailLog> logs = new ArrayList<>(25);
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("select * from jails")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new JailLog(rs.getInt("playerid"), rs.getInt("accuser"), rs.getString("reason"), rs.getTimestamp("when").getTime()));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to retrieve jail logs", e);
        }
        return logs;
    }
}
