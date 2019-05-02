package com.lucianms;

import com.lucianms.client.MapleClient;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author izarooni
 */
public class BanManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BanManager.class);
    private static Set<String> MACs;
    private static Set<String> HWIDs;

    private BanManager() {
    }

    public static void createCache(Connection con) throws SQLException {
        MACs = new HashSet<>(10);
        HWIDs = new HashSet<>(10);

        try (PreparedStatement ps = con.prepareStatement("select mac from accounts_mac where account_id = (select id from accounts where banned = 1)")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MACs.add(rs.getString("mac"));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("select hwid from accounts_hwid where account_id = (select id from accounts where banned = 1)")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HWIDs.add(rs.getString("mac"));
                }
            }
        }
        LOGGER.info("Loaded {} machine IDs and {} network addresses", HWIDs.size(), MACs.size());
    }

    public static boolean isBanned(MapleClient client) {
        return client.getHardwareIDs().stream().anyMatch(HWIDs::contains) || client.getMacs().stream().anyMatch(MACs::contains);
    }

    /**
     * Check if a MAC address has been banned from the server.
     * <p>
     * The value of this parameter can be obtained on Windows via the System32 application 'getmac.exe'
     * </p>
     * <p>
     * Sometimes multiple networking addresses may be used.
     * </p>
     *
     * @param pAddress A network physical address
     */
    public static boolean isMACBanned(String pAddress) {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select banned from accounts where (select account_id from accounts_mac where mac = ?) and banned = 1")) {
                ps.setString(1, pAddress);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        MACs.add(pAddress);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if machine '{}' is banned", pAddress, e);
        }
        return false;
    }

    /**
     * Check if a machine ID has been banned from the server
     *
     * @param machineID The machine ID
     */
    public static boolean isMachineBanned(String machineID) {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select banned from accounts where (select account_id from accounts_hwid where hwid = ?) and banned = 1")) {
                ps.setString(1, machineID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        HWIDs.add(machineID);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if machine '{}' is banned", machineID, e);
        }
        return false;
    }

    public static boolean pardonUser(String username) {
        try (Connection con = Server.getConnection()) {
            int accountID;
            try (PreparedStatement ps = con.prepareStatement("select id from accounts where name = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accountID = rs.getInt("id");
                    } else {
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("update accounts set banned = 0, ban_reason = null where name = ?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("select h.hwid, m.mac from accounts_hwid h inner join accounts_mac m where h.account_id = ? and h.account_id = m.account_id")) {
                ps.setInt(1, accountID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MACs.remove(rs.getString("mac"));
                        HWIDs.remove(rs.getString("hwid"));
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to un-ban account '{}'", username, e);
        }
        return false;
    }

    public static boolean setBanned(int accountID, String banReason) {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("update accounts set banned = ?, ban_reason = ? where id = ?")) {
                ps.setInt(1, accountID);
                ps.setString(2, banReason);
                ps.setInt(3, accountID);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("select * from accounts_mac where account_id = ?")) {
                ps.setInt(1, accountID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MACs.add(rs.getString("mac"));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("select * from accounts_hwid where account_id = ?")) {
                ps.setInt(1, accountID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        HWIDs.add(rs.getString("hwid"));
                    }
                }

            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to ban account {}", accountID, e);
        }
        return false;
    }

    public static Set<String> getMACs() {
        return MACs;
    }

    public static Set<String> getHWIDs() {
        return HWIDs;
    }
}
