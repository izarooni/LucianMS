package tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class EntryLimits {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryLimits.class);

    private EntryLimits() {
    }

    public static int getEntries(int playerID, String type) {
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("select entries from entry_limit where playerid = ? and type = ?")) {
            ps.setInt(1, playerID);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("entries");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to obtain entries values for player {} type '{}'", playerID, type, e);
        }
        return -1;
    }

    public static void incrementEntry(int playerID, String type) {
        final int entries = getEntries(playerID, type);
        if (entries == -1) {
            try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("insert into entry_limit (playerid, type, entries, last_entry) values (?, ?, 1, ?)")) {
                ps.setInt(1, playerID);
                ps.setString(2, type);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Unable to insert entry row for player {} type '{}'", playerID, type, e);
            }
        } else {
            try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("update entry_limit set entries = ?, last_entry = ? where playerid = ? and type = ?")) {
                ps.setInt(1, entries + 1);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, playerID);
                ps.setString(4, type);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Unable to update entry row for player {} type '{}", playerID, type, e);
            }
        }
    }
}
