package server.events.custom.auto;

import client.MapleCharacter;
import net.server.world.World;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class TestEvent extends GenericAutoEvent {

    private int eventMap = 100000000;

    public TestEvent(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        MapleMap map = getMapInstance(eventMap);
        if (!map.hasClock()) {
            for (MapleCharacter playerOnMap : map.getCharacters()) {
                playerOnMap.announce(MaplePacketCreator.getClock(20 * 60));
                playerOnMap.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500002), playerOnMap.getPosition());
            }

        }
    }

    @Override
    public void stop() {
        MapleMap map = getMapInstance(eventMap);
        if (!map.hasClock()) {
            for (MapleCharacter playerOnMap : map.getCharacters()) {
                playerOnMap.dropMessage(6, "The event has ended");
            }
        }

    }

}
