package server.events.custom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import client.MapleCharacter;
import net.server.Server;
import server.maps.MapleMap;
import tools.DatabaseConnection;

public class House {

	private static HashMap<Integer, House> houses = new HashMap<>();

	// table: Houses contains the following: id, ownerId, mapId, password.

	public static final int RETURN_MAP = 910000000;

	private int mapId;
	private String password;
	private MapleMap house;

	private MapleCharacter player;

	private Connection con = DatabaseConnection.getConnection();

	public House(MapleCharacter owner) {
		this.player = owner;
	}

	public boolean loadHouse() {
		try (PreparedStatement stmnt = con.prepareStatement("SELECT mapId FROM Houses WHERE OwnerId = ?")) {
			stmnt.setInt(0, player.getId());

			ResultSet rs = stmnt.executeQuery();

			this.mapId = rs.getInt("mapId");
			this.password = rs.getString("password");
			this.house = new MapleMap(mapId, player.getWorld(), player.getClient().getChannel(), RETURN_MAP, 0F);
			houses.put(player.getId(), this);

			rs.close();
			// go into the house, we load the house when talking to the NPC and
			// requesting to go into our house.

			enterHouse();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	// only thing that needs to be saved.. mhmm..
	public boolean saveHouse() {
		try (PreparedStatement stmnt = con.prepareStatement("UPDATE houses SET password = ? WHERE ownerId = ?")) {
			stmnt.setString(0, getPassword());
			stmnt.setInt(2, player.getId());

			boolean success = stmnt.execute();

			if (success) {
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean deleteHouse() {
		try (PreparedStatement stmnt = con.prepareStatement("DELETE FROM houses WHERE ownerId = ?")) {
			stmnt.setInt(0, player.getId());

			boolean success = stmnt.execute();

			if (success) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public boolean createHouse(int mapId) {
		return createHouse(mapId, "");
	}
	
	public boolean createHouse(int mapId, String password) {
		try (PreparedStatement stmnt = con.prepareStatement("INSERT INTO Houses (ownerId, mapId, password) VALUES (?, ?, ?)")) {
			stmnt.setInt(0, player.getId());
			stmnt.setInt(1, mapId);
			stmnt.setString(2, password);
			
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

	public void enterHouse() {
		if (house != null) {
			player.changeMap(getHouse());
			player.dropMessage(5, "Welcome to your home " + player.getName());
		} else {
			loadHouse();
		}
	}

	public static boolean goToHouse(MapleCharacter player, String houseOwner) {
		MapleCharacter owner = Server.getInstance().getWorld(player.getWorld()).getPlayerStorage()
				.getCharacterByName(houseOwner);
		if (owner != null) {
			if (player.getClient().getChannel() != owner.getClient().getChannel()) {
				player.getClient().changeChannel(owner.getClient().getChannel());
			}

			if (houses.containsKey(owner)) {
				House house = houses.get(owner);

				// owner has to be in the house for players to access it.
				if (house.getHouse() != null) {
					if (house.getHouse().getAllPlayer().contains(owner)) {
						player.changeMap(house.getHouse());
						player.dropMessage("Welcome to the house of " + house.getPlayer().getName());
						return true;
					}
				}
			}

		}
		return false;
	}

	public int getMapId() {
		return mapId;
	}

	public String getPassword() {
		return password;
	}

	public MapleMap getHouse() {
		return house;
	}

	public MapleCharacter getPlayer() {
		return player;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static HashMap<Integer, House> getHouses() {
		return houses;
	}

}
