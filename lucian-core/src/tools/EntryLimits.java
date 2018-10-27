package tools;

import com.lucianms.server.Server;
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

    public static class Entry {
        public final long LastEntry;
        public final int Entries;

        public Entry(long lastEntry, int entries) {
            LastEntry = lastEntry;
            Entries = entries;
        }
    }

    private EntryLimits() {
    }

    public static Entry getEntries(int playerID, String type) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select entries, last_entry from entry_limit where playerid = ? and type = ?")) {
            ps.setInt(1, playerID);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Entry(rs.getLong("last_entry"), rs.getInt("entries"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to obtain entries values for player {} type '{}': {}", playerID, type, e.getMessage());
        }
        return null;
    }

    public static void incrementEntry(int playerID, String type) {
        final Entry entry = getEntries(playerID, type);
        if (entry == null) {
            try (Connection con = Server.getConnection();
                 PreparedStatement ps = con.prepareStatement("insert into entry_limit (playerid, type, entries, last_entry) values (?, ?, 1, ?)")) {
                ps.setInt(1, playerID);
                ps.setString(2, type);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Unable to insert entry row for player {} type '{}': {}", playerID, type, e.getMessage());
            }
        } else {
            try (Connection con = Server.getConnection();
                 PreparedStatement ps = con.prepareStatement("update entry_limit set entries = ?, last_entry = ? where playerid = ? and type = ?")) {
                ps.setInt(1, entry.Entries + 1);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, playerID);
                ps.setString(4, type);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Unable to update entry row for player {} type '{}': {}", playerID, type, e.getMessage());
            }
        }
    }
}
