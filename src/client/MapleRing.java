package client;

import tools.Database;

import java.sql.*;

/**
 * @author Danny
 * @author izarooni
 */
public class MapleRing implements Comparable<MapleRing> {
    private int ringId;
    private int ringId2;
    private int partnerId;
    private int itemId;
    private String partnerName;
    private boolean equipped = false;

    public MapleRing(int id, int id2, int partnerId, int itemid, String partnername) {
        this.ringId = id;
        this.ringId2 = id2;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnername;
    }

    public static MapleRing loadFromDb(int ringId) {
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?")) {
                ps.setInt(1, ringId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new MapleRing(ringId,
                                rs.getInt("partnerRingId"),
                                rs.getInt("partnerChrId"),
                                rs.getInt("itemid"),
                                rs.getString("partnerName"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int createRing(int itemid, final MapleCharacter partner1, final MapleCharacter partner2) {
        if (partner1 == null) {
            return -2;
        } else if (partner2 == null) {
            return -1;
        }
        int[] ringID = new int[2];
        try (Connection con = Database.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, itemid);
                ps.setInt(2, partner2.getId());
                ps.setString(3, partner2.getName());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    ringID[0] = rs.getInt(1); // ID.
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, itemid);
                ps.setInt(2, ringID[0]);
                ps.setInt(3, partner1.getId());
                ps.setString(4, partner1.getName());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    ringID[1] = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE rings SET partnerRingId = ? WHERE id = ?")) {
                ps.setInt(1, ringID[1]);
                ps.setInt(2, ringID[0]);
                ps.executeUpdate();
            }
            return ringID[0];
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return ringId2;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean equipped() {
        return equipped;
    }

    public void equip() {
        this.equipped = true;
    }

    public void unequip() {
        this.equipped = false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MapleRing) {
            return ((MapleRing) o).getRingId() == getRingId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    @Override
    public int compareTo(MapleRing other) {
        if (ringId < other.getRingId()) {
            return -1;
        } else if (ringId == other.getRingId()) {
            return 0;
        }
        return 1;
    }
}
