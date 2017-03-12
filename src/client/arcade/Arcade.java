package client.arcade;

import java.awt.Point;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.MapleCharacter;
import net.server.Server;
import provider.MapleDataProviderFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;

public abstract class Arcade {
	
	protected int mapId, arcadeId, highscore;
	
	public MapleCharacter player;
	
	public Arcade(MapleCharacter player) {
		this.player = player;
	}
	
	public abstract boolean fail();
	public abstract void add();
	public abstract void onKill(int monster);
	public abstract void onHit(int monster);
	public abstract boolean onBreak(int reactor);
	
	public void start() {
		MapleMapFactory factory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")), player.getWorld(), player.getClient().getChannel());
		player.changeMap(factory.getMap(mapId), factory.getMap(mapId).getPortal(0));
		player.getMap().setMobInterval((short) 5);
		if(player.getArcade().arcadeId == 2) {
			MapleMonster toSpawn = MapleLifeFactory.getMonster(9500365);
			toSpawn.setHp(4);
			player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(-1258, 88));
		} else if(player.getArcade().arcadeId == 4) {
			MapleMonster toSpawn = MapleLifeFactory.getMonster(9500140);
			toSpawn.setHp(350000);
			player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(206, 35));
		}
		
	}
	
	public boolean saveData(int score) {
		if(score > Arcade.getHighscore(arcadeId, player)) {
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("INSERT INTO arcade (id, charid, highscore) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE highscore = ?")) {
			stmnt.setInt(1, arcadeId);
			stmnt.setInt(2, player.getId());
			stmnt.setInt(3, score);
			stmnt.setInt(4, score);
			
			return stmnt.execute();
			
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
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT highscore FROM arcade WHERE charid = ? AND id = ?")) {
			stmnt.setInt(1, player.getId());
			stmnt.setInt(2, arcadeId);
			
			stmnt.execute();
			
			ResultSet rs = stmnt.getResultSet();
			
			
			while(rs.next()) {
				highscore = rs.getInt("highscore");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return highscore;
	}
	
	public static String getTop(int arcadeId) {
		StringBuilder sb = new StringBuilder();
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT * FROM arcade WHERE id = ? ORDER BY id DESC LIMIT 50")) {
			stmnt.setInt(1, arcadeId);
			
			if(stmnt.execute()) {
				ResultSet rs = stmnt.getResultSet();
				int i = 0;
				while(rs.next()) {
					MapleCharacter user = Server.getInstance().getWorld(0).getPlayerStorage().getCharacterById(rs.getInt("charid"));
					if(user != null) {
						i++;
						sb.append("#k" + (i <= 3 ? "#b" : "") + (i > 3 && i <= 5 ? "" : "") + i +  ". " + user.getName() + " with a score of " + rs.getInt("highscore") + "\r\n");
					}
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}

}
