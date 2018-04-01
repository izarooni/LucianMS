package server.events.custom.controllers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.MapleCharacter;
import provider.MapleDataProviderFactory;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;

public class JumpQuestController {
	
	private MapleCharacter player;
	private int map, id;
	private long timeStarted;
	private int mapComingFrom;
	
	public JumpQuestController(MapleCharacter player, int id, int map, int previousMap) {
		this.player = player;
		this.map = map;
		this.id = id;
		this.mapComingFrom = previousMap;
	}
	
	public void start() {
		System.out.println("Map " + map + " came from: " + mapComingFrom + " id: " + id );
		MapleMapFactory tempFactory = new MapleMapFactory(player.getWorld(), player.getClient().getChannel());
		player.changeMap(tempFactory.getMap(map));
		
		player.getMap().getPortals().forEach((portal) -> { portal.setPortalStatus(true); }); // disabling portals.
		player.dropMessage(6, "Good luck, you'll need it.");
		
		timeStarted = System.currentTimeMillis();
	
	}
	
	public int getHighscore() {
		int highscore = 0;
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT time FROM jq_scores WHERE charid = ? AND id = ?")) {
			stmnt.setInt(1, player.getId());
			stmnt.setInt(2, id);
			
			stmnt.execute();
			
			ResultSet rs = stmnt.getResultSet();
			
			
			while(rs.next()) {
				highscore = rs.getInt("time");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return highscore;
	}
	
	
	public static String getTop(int id) {
		StringBuilder sb = new StringBuilder();
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT * FROM jq_scores WHERE id = ? ORDER BY time DESC LIMIT 50")) {
			stmnt.setInt(1, id);
			
			if(stmnt.execute()) {
				ResultSet rs = stmnt.getResultSet();
				int i = 0;
				while(rs.next()) {
					String user = MapleCharacter.getNameById(rs.getInt("charid"));
					if(user != null) {
						i++;
						sb.append("#k" + (i <= 3 ? "#b" : "") + (i > 3 && i <= 5 ? "#g" : "") + i +  ". " + user + " with a time of " + rs.getInt("time") + " seconds\r\n");
					}
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	public void end() {
		int score = getHighscore();
		try(Connection c = DatabaseConnection.getConnection(); java.sql.PreparedStatement stmnt = c.prepareStatement("INSERT INTO jq_scores (id, charid, time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE time = ?")) {
			int time = (int) ((System.currentTimeMillis() - timeStarted) / 1000);
			time = (score >= time ? time : score);
			
			stmnt.setInt(1, id);
			stmnt.setInt(2, player.getId());
			stmnt.setInt(3, time); // time in seconds
			stmnt.setInt(4, time); // time in seconds
			
			stmnt.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			player.changeMap(getReturnMap());
			
		}

	}
	
	public int getJQMap() {
		return this.map;
	}
	
	public int getReturnMap() {
		return this.mapComingFrom;
	}
	
}
