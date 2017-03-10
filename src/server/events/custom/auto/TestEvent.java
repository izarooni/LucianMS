package server.events.custom.auto;

import client.MapleCharacter;
import net.server.Server;
import server.events.AutoEvent;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class TestEvent implements AutoEvent {

	private int eventMap = 910000000;
	
	@Override
	public void onStart() {
		MapleMap map = Server.getInstance().getWorld(0).getChannel(channel).getMapFactory().getMap(eventMap);
		if(!map.hasClock()) {
			for(MapleCharacter playerOnMap : map.getCharacters()) {
				playerOnMap.announce(MaplePacketCreator.getClock(20 * 60));
			}
		
		}
	}

	@Override
	public void onEnd() {
		MapleMap map = Server.getInstance().getWorld(0).getChannel(channel).getMapFactory().getMap(eventMap);
		if(!map.hasClock()) {
			for(MapleCharacter playerOnMap : map.getCharacters()) {
				playerOnMap.changeMap(910000000);
				playerOnMap.dropMessage(6, "The event has ended");
			}
		
		}
		
	}

}
