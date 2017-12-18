package server.life;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.DatabaseConnection;
import tools.Pair;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleMonsterInformationProvider {

    private static final MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
    private final Map<Integer, List<MonsterDropEntry>> drops = new HashMap<>();
    private final List<MonsterGlobalDropEntry> globaldrops = new ArrayList<>();

    protected MapleMonsterInformationProvider() {
        retrieveGlobal();
    }

    public static MapleMonsterInformationProvider getInstance() {
        return instance;
    }

    public final List<MonsterGlobalDropEntry> getGlobalDrop() {
        return globaldrops;
    }

    private void retrieveGlobal() {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    globaldrops.add(
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
            e.printStackTrace();
        }
    }

    public final List<MonsterDropEntry> retrieveDrop(final int monsterId) {
        if (drops.containsKey(monsterId)) {
            return drops.get(monsterId);
        }
        final List<MonsterDropEntry> ret = new LinkedList<>();

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?")) {
            ps.setInt(1, monsterId);
            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    ret.add(
                            new MonsterDropEntry(
                                    rs.getInt("itemid"),
                                    rs.getInt("chance"),
                                    rs.getInt("minimum_quantity"),
                                    rs.getInt("maximum_quantity"),
                                    rs.getShort("questid")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ret;
        }
        drops.put(monsterId, ret);
        return ret;
    }

    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<>();
        MapleData data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (MapleData mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public static String getMobNameFromID(int id) {
        MapleMonster monster = MapleLifeFactory.getMonster(id);
        return monster != null ? monster.getName() : null;
    }

    public final void reload() {
        drops.clear();
        globaldrops.clear();
        retrieveGlobal();
    }
}