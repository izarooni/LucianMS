package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import tools.DatabaseConnection;


/**
 * @author Lucas Ouwens
 *
 */
public class Occupations {

	
	private Occupation occupation = Occupation.none;
	private MapleCharacter player = null;
	
	public Occupations(MapleCharacter player) {
		this.player = player;
	}
	
	
	private enum Occupation {
		none,
		destruction,
			alchemy,
				assassination,
					merching;
	}
	
	public boolean save() {
		try(Connection con = DatabaseConnection.getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE maple_maplelife.characters SET occupation = ? WHERE id = ?") ) {
			statement.setInt(1, occupation.ordinal());
			statement.setInt(2, player.getId());

			return statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public MapleCharacter getPlayer() {
		return this.player;
	}
	
	public Occupations setOccupation(Occupation occupation) {
		this.occupation = occupation;
		
		return this; // For method chaining!
	}
	
	public Occupation getOccupation() {
		return this.occupation;
	}
	
}
