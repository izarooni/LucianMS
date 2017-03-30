package server.events.custom.auto;

import client.MapleCharacter;
import net.server.Server;
import server.events.AutoEvent;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class TestEvent2 implements AutoEvent {

	private int eventMap = 100000000;
	
	@Override
	public void onStart() {
		MapleMap map = Server.getInstance().getWorld(0).getChannel(channel).getMapFactory().getMap(eventMap);
		if(!map.hasClock()) {
			for(MapleCharacter playerOnMap : map.getCharacters()) {
				playerOnMap.announce(MaplePacketCreator.getClock(20 * 60));
				playerOnMap.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500002), playerOnMap.getPosition());
			}
		
		}
	}

	@Override
	public void onEnd() {
		MapleMap map = Server.getInstance().getWorld(0).getChannel(channel).getMapFactory().getMap(eventMap);
		if(!map.hasClock()) {
			for(MapleCharacter playerOnMap : map.getCharacters()) {
				playerOnMap.dropMessage(6, "The event has ended");
			}
		
		}
		
	}

}
