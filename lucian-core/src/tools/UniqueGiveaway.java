package tools;

import com.lucianms.server.Server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UniqueGiveaway {

    public enum Type {
        BETA_REWARD
    }

    private UniqueGiveaway() {
    }

    public static void createData(String ip, String hdd, String description) {
        try (Connection con = Server.getConnection() ;
             PreparedStatement ps = con.prepareStatement("insert into unique_giveaways values (? ,? ,?)")) {
            ps.setString(1, ip);
            ps.setString(2, hdd);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkWithBoth(String ip, String hdd, String description) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from unique_giveaways where (mac = ? or hdd = ?) and description = ?")) {
            ps.setString(1, ip);
            ps.setString(2, hdd);
            ps.setString(3, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkWithIPAddress(String ip, String description) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from unique_giveaways where mac = ? and description = ?")) {
            ps.setString(1, ip);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkWithHDD(String hdd, String description) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from unique_giveaways where hdd = ? and description = ?")) {
            ps.setString(1, hdd);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<Type> getFromIPAddress(String ip) {
        ArrayList<Type> types = new ArrayList<>();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from unique_giveaways where mac = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.add(Type.valueOf(rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }

    public static List<Type> getFromHDDAddress(String ip) {
        ArrayList<Type> types = new ArrayList<>();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from unique_giveaways where hdd = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.add(Type.valueOf(rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }
}
