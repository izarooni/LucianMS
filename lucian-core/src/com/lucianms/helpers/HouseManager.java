package com.lucianms.helpers;

import com.lucianms.features.House;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class HouseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HouseManager.class);
    private static ConcurrentHashMap<Integer, House> houses = new ConcurrentHashMap<>();

    private HouseManager() {
    }

    private static House loadHouse(int ownerID) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from houses where ownerID = ?")) {
            ps.setInt(1, ownerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new House(ownerID, rs.getInt("mapID"), rs.getString("password"), rs.getTimestamp("bill").getTime());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to load house information for owner {}", ownerID, e);
        }
        return null;
    }

    public static House getHouse(int ownerID) {
        return houses.computeIfAbsent(ownerID, HouseManager::loadHouse);
    }

    public static int loadHouses() {
        // initialize with previous size for load factor calculation
        houses = new ConcurrentHashMap<>((int) ((houses.size() / 0.75f) + 1));
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from houses")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    House house = new House(rs.getInt("ownerID"), rs.getInt("mapID"), rs.getString("password"), rs.getTimestamp("bill").getTime());
                    houses.put(house.getOwnerID(), house);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to load house information", e);
        }
        return houses.size();
    }

    public static House createHouse(int ownerID, int mapID, String password) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("insert into houses (ownerID, mapID, `password`, bill) values (?, ?, ?, ?)")) {
            // does this work
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
            Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());

            ps.setInt(1, ownerID);
            ps.setInt(2, mapID);
            ps.setString(3, password);
            ps.setTimestamp(4, timestamp);
            ps.executeUpdate();

            House house = new House(ownerID, mapID, password, timestamp.getTime());
            houses.put(ownerID, house);
            return house;
        } catch (SQLException e) {
            LOGGER.error("Unable to insert new house row information", e);
            throw new RuntimeException();
        }
    }

    public static void removeHouse(int ownerID) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("delete from houses where ownerID = ?")) {
            ps.setInt(1, ownerID);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Unable to delete house via owner ID {}", ownerID, e);
        } finally {
            houses.remove(ownerID);
        }
    }

    public static void updateRent(int ownerID) {
        House house = getHouse(ownerID);
        if (house == null) {
            LOGGER.warn("Unable to update rent for player {} (doesn't exist)", ownerID);
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        house.setBillDate(calendar.getTimeInMillis());
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("update houses set bill = ? where ownerID = ?")) {
            ps.setLong(1, house.getBillDate());
            ps.setInt(2, ownerID);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Unable to update rent for player {}", ownerID, e);
        }
    }

    public static void updatePassword(int ownerID, String password) {
        House house = getHouse(ownerID);
        if (house == null) {
            LOGGER.warn("Unable to update password for player {} (doesn't exist)", ownerID);
            return;
        }
        house.setPassword(password);
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("update houses set password = ? where ownerID = ?")){
                ps.setString(1, password);
                ps.setInt(2, ownerID);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to update password for player {}", ownerID, e);
        }
    }
}
