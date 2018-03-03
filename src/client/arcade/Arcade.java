package client.arcade;

import client.MapleCharacter;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Arcade {

    int mapId, arcadeId;
    Task respawnTask = null;
    double rewardPerKill;
    int itemReward;

    public MapleCharacter player;

    Arcade(MapleCharacter player) {
        this.player = player;
    }

    public abstract boolean fail();

    public abstract void add();

    public abstract void onKill(int monster);

    public abstract void onHit(int monster);

    public abstract boolean onBreak(int reactor);

    public synchronized void start() {
        MapleMapFactory factory = new MapleMapFactory(player.getWorld(), player.getClient().getChannel());
        player.changeMap(factory.getMap(mapId), factory.getMap(mapId).getPortal(0));

        // disable portals, we do not want them to leave the map. TODO: disable commands
        player.getMap().getPortals().forEach((portal) -> portal.setDisabled(true));

        player.getMap().setMobInterval((short) 5);

        respawnTask = TaskExecutor.createRepeatingTask(() -> factory.getMaps().forEach(MapleMap::respawn), 10000, 1000);

        player.getMap().toggleDrops();
        if(player.getArcade().arcadeId == 0) {
            player.getMap().toggleDrops(); // drops should be enabled in loot-a-holic
        } else if (player.getArcade().arcadeId == 2) {
            MapleMonster toSpawn = MapleLifeFactory.getMonster(9500365);
            toSpawn.setHp(4);
            player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(-2255, -1880));
        } else if (player.getArcade().arcadeId == 4) {
            MapleMonster toSpawn = MapleLifeFactory.getMonster(9500140);
            toSpawn.setHp(350000);
            player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(206, 35));
        } else if (player.getArcade().arcadeId == 5) {
            player.announce(MaplePacketCreator.getClock(150));
            TaskExecutor.createTask(this::fail, 150000);

        }

    }



    public boolean saveData(int score) {
        if (score > Arcade.getHighscore(arcadeId, player)) {
            try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("INSERT INTO arcade (id, charid, highscore) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE highscore = ?")) {
                stmnt.setInt(1, arcadeId);
                stmnt.setInt(2, player.getId());
                stmnt.setInt(3, score);
                stmnt.setInt(4, score);
                stmnt.execute();

                return true;

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }
        return false;
    }

    public static int getHighscore(int arcadeId, MapleCharacter player) {
        int highscore = 0;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT highscore FROM arcade WHERE charid = ? AND id = ?")) {
            stmnt.setInt(1, player.getId());
            stmnt.setInt(2, arcadeId);

            stmnt.execute();

            ResultSet rs = stmnt.getResultSet();


            while (rs.next()) {
                highscore = rs.getInt("highscore");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highscore;
    }

    public static String getTop(int arcadeId) {
        StringBuilder sb = new StringBuilder();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT * FROM arcade WHERE id = ? ORDER BY highscore DESC LIMIT 50")) {
            stmnt.setInt(1, arcadeId);

            if (stmnt.execute()) {
                ResultSet rs = stmnt.getResultSet();
                int i = 0;
                while (rs.next()) {
                    String user = MapleCharacter.getNameById(rs.getInt("charid"));
                    if (user != null) {
                        i++;
                        sb.append("#k" + (i <= 3 ? "#b" : "") + (i > 3 && i <= 5 ? "" : "") + i + ". " + user + " with a score of " + rs.getInt("highscore") + "\r\n");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public int getMapId() {
        return mapId;
    }

    public int getId() {
        return arcadeId;
    }

    public MapleCharacter getPlayer() {
        return player;
    }
}
