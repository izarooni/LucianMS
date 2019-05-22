package com.lucianms.client;

import com.lucianms.server.Server;
import tools.Database;

import java.sql.*;

/**
 * @author Danny
 * @author izarooni
 */
public class MapleRing {

    private int ringId;
    private int partnerRingID;
    private int partnerID;
    private int itemId;
    private String partnerName;
    private boolean equipped;

    public MapleRing(int ringID, int partnerRingID, int partnerID, int itemID, String partnerName) {
        this.ringId = ringID;
        this.partnerRingID = partnerRingID;
        this.partnerID = partnerID;
        this.itemId = itemID;
        this.partnerName = partnerName;
    }

    public static MapleRing load(int ringId) throws SQLException {
        MapleRing ring = null;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE id = ?")) {
                ps.setInt(1, ringId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ring = new MapleRing(ringId,
                                rs.getInt("partnerRingId"),
                                rs.getInt("partnerChrId"),
                                rs.getInt("itemid"),
                                rs.getString("partnerName"));
                    }
                }
            }
            if (ring != null) {
                // ring exists, so find its partner
                try (PreparedStatement ps = con.prepareStatement("select * from rings where id = ?")) {
                    ps.setInt(1, ring.getPartnerRingId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            // partner doens't exist, so remove the ring
                            Database.executeSingle(con, "delete from rings where id = ?", ringId);
                            return null;
                        }
                    }
                }
            }
        }
        return ring;
    }

    public static int create(int itemID, final MapleCharacter partner1, final MapleCharacter partner2) throws SQLException {
        if (partner1 == null) {
            return -2;
        } else if (partner2 == null) {
            return -1;
        }
        int[] ringID = new int[2];
        try (Connection con = partner1.getClient().getWorldServer().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerChrId, partnername) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, itemID);
                ps.setInt(2, partner2.getId());
                ps.setString(3, partner2.getName());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    ringID[0] = rs.getInt(1); // ID.
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO rings (itemid, partnerRingId, partnerChrId, partnername) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, itemID);
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
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return partnerRingID;
    }

    public int getPartnerChrId() {
        return partnerID;
    }

    public int getItemId() {
        return itemId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    public boolean equipped() {
        return equipped;
    }

    public void unequip() {
        this.equipped = false;
    }
}
