package com.lucianms.server.life;

import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class MapleMonsterInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleMonsterInformationProvider.class);
    private static final Map<Integer, ArrayList<MonsterDropEntry>> A_DROP_DATA = new HashMap<>();
    private static final List<MonsterGlobalDropEntry> A_GLOBAL_DROPS = new ArrayList<>();

    private MapleMonsterInformationProvider() {
    }

    public static List<MonsterGlobalDropEntry> getGlobalDrop() {
        return A_GLOBAL_DROPS;
    }

    public static List<MonsterDropEntry> retrieveDrop(final int monsterId) {
        if (A_DROP_DATA.containsKey(monsterId)) {
            return A_DROP_DATA.get(monsterId);
        }
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?")) {
            ps.setInt(1, monsterId);
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<MonsterDropEntry> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(new MonsterDropEntry(
                            rs.getInt("itemid"),
                            rs.getInt("chance"),
                            rs.getInt("minimum_quantity"),
                            rs.getInt("maximum_quantity"),
                            rs.getShort("questid")));
                }
                A_DROP_DATA.put(monsterId, ret);
                return ret;
            }
        } catch (SQLException e) {
            LOGGER.error("Faile to retrieve drop data for monster {}", monsterId, e);
            A_DROP_DATA.remove(monsterId);
        }
        return Collections.emptyList();
    }

    public static void createGlobalCache() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    A_GLOBAL_DROPS.add(
                            new MonsterGlobalDropEntry(
                                    rs.getInt("itemid"),
                                    rs.getInt("chance"),
                                    rs.getInt("continent"),
                                    rs.getByte("dropType"),
                                    rs.getInt("minimum_quantity"),
                                    rs.getInt("maximum_quantity"),
                                    rs.getShort("questid")));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to retrieve global drop data", e);
        }
    }

    public static void clearCache() {
        A_DROP_DATA.clear();
        A_GLOBAL_DROPS.clear();

        createGlobalCache();
    }
}