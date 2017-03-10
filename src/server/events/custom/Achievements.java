package server.events.custom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.Item;
import tools.DatabaseConnection;

public class Achievements {

	MapleCharacter player;
	
	
	private static enum AchievementHolder {
		ACHIEVEMENT_1(0, false),
			ACHIEVEMENT_2(1, false),
				ACHIEVEMENT_3(2, true);
		
		
		private int holder;
		private boolean daily;
		AchievementHolder(int holder, boolean daily) {
			this.holder = holder;
			this.daily = daily;
		}
		
		public static AchievementHolder get(int achievementId) {
			for(AchievementHolder ac : values()) {
				if(ac.holder == achievementId) {
					return ac;
				}
			}
			return null;
		}
		
		public boolean isDaily() {
			return daily;
		}
		
		
	}
	
	public Achievements(MapleCharacter player) {
		this.player = player;
	}
	
	public void unlockAchievement(int achievementId) {
		String[] alrUnlocked = getAll(false, true);
		String[] dailies = getAll(true, true);
		
		AchievementHolder holder = AchievementHolder.get(achievementId);
		
		boolean execute = true;
		Timestamp timestamp = getTimestamp(holder.holder);
		if(holder.isDaily() && dailies != null && dailies.length > 1 || !holder.isDaily() && alrUnlocked != null && alrUnlocked.length > 1) {
		for(int i = 0; i < (holder.isDaily() ? alrUnlocked.length : dailies.length); i++) {
			if(Integer.parseInt(alrUnlocked[i]) == achievementId || holder.isDaily() && timestamp.before(new Timestamp(timestamp.getTime() + 86400000))) {
				execute = false;
			}
		}
		}
		
		if(execute) {
			try (Connection con = DatabaseConnection.getConnection(); PreparedStatement statement = con.prepareStatement("INSERT INTO achievements (character_id, achievement_id, daily, timestamp) VALUES (?, ?, ?, ?)")) {
				statement.setInt(1, player.getId());
				statement.setInt(2, achievementId);
				statement.setBoolean(3, AchievementHolder.get(achievementId).isDaily());
				statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				if(statement.execute()) {
					player.dropMessage("You unlocked the achievement: " + AchievementHolder.get(achievementId).name().replaceAll("_", " ").toLowerCase());
				}
		
			
			} catch(SQLException e) {
				// nah
			}
		}
	}
	
	private Timestamp getTimestamp(int achievementId) {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM achievements WHERE character_id = ? AND achievement_id = ?")) {
			statement.setInt(1, player.getId());
			statement.setInt(2, achievementId);
			
			statement.execute();
			
			ResultSet rs = statement.getResultSet();
			
			if(rs.next()) {
				return rs.getTimestamp("timestamp");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public String[] getAll(boolean daily, boolean unlocked) {
		StringBuilder anotherBuilder = new StringBuilder();
		StringBuilder builder = new StringBuilder();
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM achievements WHERE character_id = ? AND daily = ?")) {
			statement.setInt(1, player.getId());
			statement.setBoolean(2, daily);
			statement.execute();
			
			ResultSet rs = statement.getResultSet();
			
			while(rs.next()) {
				builder.append(rs.getInt("achievement_id") + ";");
			}
			
			if(!unlocked) {
				String[] splitted = builder.toString().split(";");
				for(AchievementHolder ac : AchievementHolder.values()) {
					boolean exists = false;
					for(int i = 0; i < splitted.length; i++) {
						if(ac.holder == Integer.parseInt(splitted[i]) && ac.isDaily() == daily) {
							exists = true;
						}
					}
					if(exists) {
						anotherBuilder.append(ac.holder + ";");
					}
				}
			}
			
			return unlocked ? builder.toString().split(";") : anotherBuilder.toString().split(";");
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getAll() {
		List<Integer> gained = new ArrayList<Integer>();
		StringBuilder builder = new StringBuilder();
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM achievements WHERE character_id = ?")) {
			statement.setInt(1, player.getId());
			
			if(statement.execute()) {
				ResultSet set = statement.getResultSet();
				
				while(set.next()) {
					int id = set.getInt("achievement_id");
					gained.add(id);
					builder.append("#b" + AchievementHolder.get(id).name().toLowerCase().replaceAll("_", " ") + "\r\n");
				}
				
				for(AchievementHolder holder : AchievementHolder.values()) {
					if(!(gained.contains(holder.holder))) {
						builder.append("#r" + holder.name().toLowerCase().replace("_", " ") + "\r\n");
					}
				}
				
			}
			
			return builder.toString();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public void giveReward(int mesos, Item... items) {
		
	}
	
	public void giveReward(int mesos, Equip... equips) {
		
	}
	
	
	public void giveReward(int mesos) {
		
	}
	
}
