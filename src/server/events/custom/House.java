package server.events.custom;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import client.MapleCharacter;
import net.server.Server;
import provider.MapleDataProviderFactory;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;

/**
 * 
 * @author Lucas (lucasdieswagger)
 * @version 0.34
 * 
 * An useless system, seriously, why are you even adding this to your server?
 */

public class House {

	// all houses that are loaded, are stored in here
	private static HashMap<Integer, House> houses = new HashMap<>();

	// table: Houses contains the following: id, ownerId, mapId, password.

	// if you leave your house, or if the owner leaves the house, you get warped here
	public static final int RETURN_MAP = 910000000;

	// the map id and password of your house
	private int mapId;
	private String password;
	
	// map stuff
	private MapleMap map;
	private MapleMapFactory mapFactory;

	// the house owner
	private MapleCharacter player;


	/**
	 * @param owner of the house
	 */
	public House(MapleCharacter owner) {
		this.player = owner;
	}

	
	/**
	 * Load the house of the specific player this object belongs to, sets up the house of the user
	 * @return true or false depending on if the user has a house of not
	 */
	private boolean loadHouse() {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT * FROM houses WHERE OwnerId = ?")) {
			stmnt.setInt(1, player.getId());
			
			if(stmnt.execute()) {
				
				ResultSet rs = stmnt.getResultSet();
				if(!rs.next()) {
					rs.close();
					return false;
				}
				// because you can't warp to the map by creating a new maplemap, null stuff..
				this.mapFactory = new MapleMapFactory(player.getWorld(), player.getClient().getChannel());
				
				
				this.mapId = rs.getInt("mapId");
				this.password = rs.getString("password");
				this.map = mapFactory.getMap(mapId);
				
				houses.put(player.getId(), this);

				rs.close();
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * @param player the user you want to check of if they have a house
	 * @return true/false depending on if the user has a house
	 */
	public static boolean hasHouse(MapleCharacter player) {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("SELECT * FROM houses WHERE OwnerId = ?")) {
			stmnt.setInt(1, player.getId());
			
			 if(stmnt.execute()) {
				 return stmnt.getResultSet().next();			 
			 }
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Save the password of the house, that's the only thing that needs to be saved
	 * @return the success of executing the statement
	 */
	public boolean saveHouse() {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("UPDATE houses SET password = ? WHERE ownerId = ?")) {
			stmnt.setString(1, password);
			stmnt.setInt(2, player.getId());

			return stmnt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Remove the house from the user this object belongs to, if there's a reason for them to delete the house, they can.
	 * @return true or false depending on if the execution went successfully
	 */
	public boolean deleteHouse() {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("DELETE FROM houses WHERE ownerId = ?")) {
			stmnt.setInt(1, player.getId());

			return stmnt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * 
	 * @param mapId the map id to create the house out of
	 * @return true/false depending on if the creation was successful
	 */
	public boolean createHouse(int mapId) {
		return createHouse(mapId, "");
	}
	
	/**
	 * 
	 * @param mapId the map id the house needs to be created in
	 * @param password the password you want to add to your house.
	 * @return true/false depending on if it was successful
	 */
	public boolean createHouse(int mapId, String password) {
		try (Connection con = DatabaseConnection.getConnection(); PreparedStatement stmnt = con.prepareStatement("INSERT INTO houses (ownerId, mapId, password) VALUES (?, ?, ?)")) {
			stmnt.setInt(1, player.getId());
			stmnt.setInt(2, mapId);
			stmnt.setString(3, password);
			
			boolean success = stmnt.execute();
			
			if(success) {
				loadHouse();
				return true;
			}
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Go to your house
	 */
	public void enterHouse() {
		if (loadHouse()) {
			getMap().getPortals().forEach((portal) -> {
				portal.setDisabled(true); // disable portals, boiiiii.
			});
			player.changeMap(getMap());
			player.dropMessage(5, "Welcome to your home, " + player.getName() + ".");
		}
	}

	/**
	 * Go to the house of a specified target.
	 * @param player the user trying to go to the house
	 * @param houseOwner the owner of the house by name
	 * @return true/false depending on the success of the house joining
	 */
	public static boolean goToHouse(MapleCharacter player, String houseOwner) {
		MapleCharacter owner = Server.getInstance().getWorld(player.getWorld()).getPlayerStorage()
				.getCharacterByName(houseOwner);
		if (owner != null) {
			if (player.getClient().getChannel() != owner.getClient().getChannel()) {
				player.getClient().changeChannel(owner.getClient().getChannel());
			}
			
			if (houses.containsKey(owner.getId())) {
				House house = houses.get(owner.getId());

				// owner has to be in the house for players to access it.
				if (house.getMap() != null) {
					if (house.getMap().getAllPlayer().contains(owner)) {
						player.changeMap(house.getMap());
						player.dropMessage("Welcome to the house of " + house.getPlayer().getName());
						return true;
					}
				}
			}

		}
		return false;
	}

	/**
	 * @return the map id of the house of the player
	 */
	public int getMapId() {
		return mapId;
	}
	
	/**
	 * @return the password of the players house
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the house of the player
	 */
	public MapleMap getMap() {
		return map;
	}

	/**
	 * @return the owner of the house
	 */
	public MapleCharacter getPlayer() {
		return player;
	}
	
	/**
	 * @param password the password you wish to set for the house
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * @return the MapleMapFactory
	 */
	public MapleMapFactory getMapFactory() {
		return this.mapFactory;
	}

	/**
	 * @return the houses hashmap for indirect access
	 */
	public static HashMap<Integer, House> getHouses() {
		return houses;
	}	

}
