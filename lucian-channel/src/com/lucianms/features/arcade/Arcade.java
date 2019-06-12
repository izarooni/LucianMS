package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.features.GenericEvent;
import com.lucianms.scheduler.Task;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.Server;
import com.lucianms.server.maps.MapleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author Lucasdieswagger
 * @author izarooni
 */
public abstract class Arcade extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(Arcade.class);

    int arcadeID;
    Task respawnTask;

    public final MapleMap getMap(MapleClient client, int mapID, Consumer<FieldBuilder> consumer) {
        FieldBuilder builder = new FieldBuilder(client.getWorld(), client.getChannel(), mapID).loadFootholds().loadPortals();
        if (consumer != null) consumer.accept(builder);
        MapleMap build = builder.build();
        build.setInstanced(true);
        return build;
    }

    public abstract void start();

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (!destination.isInstanced()) {
            unregisterPlayer(player);
        }
        return false;
    }

    public void saveData(int playerID, int score) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO arcade (id, charid, highscore) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE highscore = ?")) {
            ps.setInt(1, arcadeID);
            ps.setInt(2, playerID);
            ps.setInt(3, score);
            ps.setInt(4, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to save high-score for player {} arcade {}", playerID, arcadeID, e);
        }
    }

    public static int getHighscore(int playerID, int arcadeID) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT highscore FROM arcade WHERE charid = ? AND id = ?")) {
            ps.setInt(1, playerID);
            ps.setInt(2, arcadeID);
            ps.executeQuery();
            try (ResultSet rs = ps.getResultSet()) {
                if (rs.next()) {
                    return rs.getInt("highscore");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get high-score for player {} arcade {}", playerID, arcadeID, e);
        }
        return 0;
    }

    public static String getTop(int arcadeID) {
        StringBuilder sb = new StringBuilder();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM arcade WHERE id = ? ORDER BY highscore DESC LIMIT 50")) {
            ps.setInt(1, arcadeID);
            try (ResultSet rs = ps.executeQuery()) {
                int i = 0;
                while (rs.next()) {
                    String user = MapleCharacter.getNameById(rs.getInt("charid"));
                    if (user != null) {
                        i++;
                        sb.append("#k").append(i <= 3 ? "#b" : "").append(i > 3 && i <= 5 ? "" : "").append(i).append(". ").append(user).append(" with a score of ").append(rs.getInt("highscore")).append("\r\n");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get rankings for arcade {}", arcadeID, e);
        }
        return sb.toString();
    }
}