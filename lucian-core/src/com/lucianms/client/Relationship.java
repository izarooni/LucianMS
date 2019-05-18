package com.lucianms.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class Relationship {

    public enum Status {
        Single, Engaged, Married
    }

    private Status status = Status.Single;
    private int marriageId;
    private int groomId;
    private String groomUsername;
    private int brideId;
    private String brideUsername;
    private int engagementBoxId; // engagement box that was used to propose

    public void reset() {
        setMarriageId(0);
        setEngagementBoxId(0);
        setGroomId(0);
        setBrideId(0);
        setStatus(Relationship.Status.Single);
    }

    public void load(Connection con, int marriageId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("select * from marriages where id = ?")) {
            ps.setInt(1, marriageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.marriageId = marriageId;
                    this.status = Status.values()[rs.getInt("married")];
                    this.groomId = rs.getInt("groom");
                    this.brideId = rs.getInt("bride");
                    this.engagementBoxId = rs.getInt("engagementbox");
                } else {
                    return;
                }
            }
        }
        // saving overhead of querying the usernames in npcs several times
        try (PreparedStatement ps = con.prepareStatement("select id, name from characters where id = ? or id = ?")) {
            ps.setInt(1, groomId);
            ps.setInt(2, brideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("id") == groomId) {
                        groomUsername = rs.getString("name");
                    } else if (rs.getInt("id") == brideId) {
                        brideUsername = rs.getString("name");
                    }
                }
            }
        }
    }

    public void save(Connection con) throws SQLException {
        if (marriageId == 0 && status != Status.Single) {
            try (PreparedStatement ps = con.prepareStatement("insert into marriages values (default, ?, ?, ? ,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, groomId);
                ps.setInt(2, brideId);
                ps.setInt(3, engagementBoxId);
                ps.setInt(4, status.ordinal());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        marriageId = rs.getInt(1);
                    }
                }
            }
        } else {
            try (PreparedStatement ps = con.prepareStatement("update marriages set married = ? where id = ?")) {
                ps.setInt(1, status.ordinal());
                ps.setInt(2, marriageId);
                ps.executeUpdate();
            }
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.Single) {
            marriageId = 0;
            groomId = 0;
            brideId = 0;
            engagementBoxId = 0;
        }
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(int marriageId) {
        this.marriageId = marriageId;
    }

    public int getGroomId() {
        return groomId;
    }

    public void setGroomId(int groomId) {
        this.groomId = groomId;
    }

    public String getGroomUsername() {
        return groomUsername;
    }

    public int getBrideId() {
        return brideId;
    }

    public void setBrideId(int brideId) {
        this.brideId = brideId;
    }

    public String getBrideUsername() {
        return brideUsername;
    }

    public int getEngagementBoxId() {
        return engagementBoxId;
    }

    public void setEngagementBoxId(int engagementBoxId) {
        this.engagementBoxId = engagementBoxId;
    }

    public int getPartnerID(MapleCharacter player) {
        return groomId == player.getId() ? brideId : groomId;
    }
}
