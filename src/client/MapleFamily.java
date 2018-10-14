package client;

import tools.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jay Estrella :3 (Mr.Trash)
 * @author izarooni
 */
public class MapleFamily {
    private static int id;
    private static Map<Integer, MapleFamilyEntry> members = new HashMap<Integer, MapleFamilyEntry>();

    public MapleFamily(int cid) {
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT familyid FROM family_character WHERE cid = ?")) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt("familyid");
                    }
                }
                getMapleFamily();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void getMapleFamily() {
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM family_character WHERE familyid = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MapleFamilyEntry ret = new MapleFamilyEntry();
                        ret.setFamilyId(id);
                        ret.setRank(rs.getInt("rank"));
                        ret.setReputation(rs.getInt("reputation"));
                        ret.setTotalJuniors(rs.getInt("totaljuniors"));
                        ret.setFamilyName(rs.getString("name"));
                        ret.setJuniors(rs.getInt("juniorsadded"));
                        ret.setTodaysRep(rs.getInt("todaysrep"));
                        int cid = rs.getInt("cid");
                        ret.setChrId(cid);
                        members.put(cid, ret);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MapleFamilyEntry getMember(int cid) {
        if (members.containsKey(cid)) {
            return members.get(cid);
        }
        return null;
    }

    public Map<Integer, MapleFamilyEntry> getMembers() {
        return members;
    }
}
