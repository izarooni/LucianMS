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

        try (PreparedStatement ps = con.prepareStatement("select mac from macbans")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MACs.add(rs.getString("mac"));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("select hwid from hwidbans")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HWIDs.add(rs.getString("hwid"));
                }
            }
        }
        LOGGER.info("Loaded {} machine IDs and {} network addresses", HWIDs.size(), MACs.size());
    }

    public static boolean isBanned(MapleClient client) {

        //LOGGER.error(HWIDs.toString());




        String account = client.getAccountName();
        int banned = 0;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT banned FROM accounts WHERE id=?")){
                ps.setString(1, account);
                try(ResultSet rs = ps.executeQuery()){
                    if (rs.next()){
                        LOGGER.error("Banned hehe");
                        banned = rs.getInt("banned");

                    }
                }
            }
        }catch(SQLException e) {
            LOGGER.error("Failed to check if Banned");

        }
        /*if(banned == 1){
            return true;
        }else {
            return false;
        }

         */
        //return false;
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
            try (PreparedStatement ps = con.prepareStatement("select mac from macbans where mac = ?")) {
                ps.setString(1, pAddress);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        //MACs.add(pAddress);
                        LOGGER.error(pAddress);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if machine '{}' is banned", pAddress, e);
        }

        //LOGGER.error(pAddress);
        return false;
    }

    /**
     * Check if a machine ID has been banned from the server
     *
     * @param machineID The machine ID
     */
    public static boolean isMachineBanned(String machineID) {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select hwid from hwidbans where hwid = ?")) {
                ps.setString(1, machineID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        //HWIDs.add(machineID);
                        //LOGGER.error(machineID);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if machine '{}' is banned", machineID, e);
        }

        //LOGGER.error(machineID);
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
            try (PreparedStatement ps = con.prepareStatement("update accounts set banned = 1, ban_reason = ? where id = ?")) {
                ps.setString(1, banReason);
                ps.setInt(2, accountID);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("select * from accounts_mac where account_id = ?")) {
                ps.setInt(1, accountID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        //MACs.add(rs.getString("mac"));
                        String mac = rs.getString("mac");
                        try (PreparedStatement ps2 = con.prepareStatement("insert into macbans (mac) values (?)")) {
                            ps2.setString(1, mac);
                            ps2.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("select * from accounts_hwid where account_id = ?")) {
                ps.setInt(1, accountID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        //HWIDs.add(rs.getString("hwid"));
                        String hwid=rs.getString("hwid");
                        try (PreparedStatement ps2 = con.prepareStatement("insert into hwidbans (hwid) values (?)")) {
                            ps2.setString(1,hwid);
                            ps2.executeUpdate();
                        }
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
