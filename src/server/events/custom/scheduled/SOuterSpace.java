package server.events.custom.scheduled;

import client.MapleCharacter;
import net.server.channel.Channel;
import net.server.world.World;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

/**
 * @author izarooni
 */
public class SOuterSpace extends SAutoEvent {

    private static final int MapId = 98;
    private static final int MonsterId = 9895259;
    private static final int[][] PortalPositions = {{162, 881}, {2360, 881}, {3119, 581}, {3831, 821}};

    private static int RunningInstance = 0;

    private final World world;
    private final int instanceId;

    public SOuterSpace(World world) {
        this.world = world;
        instanceId = RunningInstance++;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName() + instanceId;
    }

    @Override
    public long getInterval() {
        // 2 hours
        return 1000 * 60 * 60 * 2;
    }

    @Override
    public void run() {
        Channel channel = world.getChannel(1);
        MapleMap eventMap = channel.getMapFactory().getMap(MapId);
        int[] ipos = PortalPositions[Randomizer.nextInt(PortalPositions.length)];
        Point pos = new Point(ipos[0], ipos[1]);
        MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);
        eventMap.spawnMonsterOnGroudBelow(monster, pos);
        channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The Space Slime has spawned in the Outer Space, Planet Lucian"));
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
    }
}
