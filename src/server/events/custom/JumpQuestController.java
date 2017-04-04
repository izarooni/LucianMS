package server.events.custom;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import client.MapleCharacter;
import net.server.Server;
import provider.MapleDataProviderFactory;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;

public class JumpQuestController {
	
	private MapleCharacter player;
	private int map, id;
	private long timeStarted;
	private int mapComingFrom;
	
	public JumpQuestController(MapleCharacter player, int id, int map) {
		this.player = player;
		this.map = map;
		this.id = id;
	}
	
	public void start() {
		MapleMapFactory tempFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")), player.getWorld(), player.getClient().getChannel());
		player.changeMap(tempFactory.getMap(map));
		
		player.getMap().getPortals().forEach((portal) -> { portal.setDisabled(true); }); // disabling portals.
		player.dropMessage(6, "Good luck, you'll need it.");
		
		timeStarted = System.currentTimeMillis();
	
	}
	
	
	public void end() {
		try(Connection c = DatabaseConnection.getConnection(); java.sql.PreparedStatement stmnt = c.prepareStatement("INSERT INTO jq_scores (id, charid, time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE time = ?")) {
			stmnt.setInt(1, id);
			stmnt.setInt(2, player.getId());
			stmnt.setInt(3, (int) (System.currentTimeMillis() - timeStarted) / 1000); // time in seconds
			
			stmnt.execute();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			MapleMap previousMapFactoryMap = Server.getInstance().getChannel(player.getWorld(), player.getClient().getChannel()).getMapFactory().getMap(mapComingFrom);
			player.changeMap(previousMapFactoryMap);
			
		}

	}
}
