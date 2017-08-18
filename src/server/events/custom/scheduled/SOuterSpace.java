package server.events.custom.scheduled;

import client.MapleCharacter;
import net.server.channel.Channel;
import net.server.world.World;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MonsterListener;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

/**
 * Should only ever been one instance per world
 *
 * @author izarooni
 */
public class SOuterSpace extends SAutoEvent {

    private static final int MapId = 98;
    private static final int MonsterId = 9895259;
    private static final int[][] PortalPositions = {{162, 881}, {2360, 881}, {3119, 581}, {3831, 821}};

    private final World world;
    private final boolean[] finished;
    private boolean open = false;

    public SOuterSpace(World world) {
        this.world = world;
        this.finished = new boolean[world.getChannels().size()];
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isFinished() {
        for (boolean b : finished) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public long getInterval() {
        // 2 hours
        return 1000 * 60 * 60 * 2;
    }

    @Override
    public void run() {
        // previous event was unable to finish?
        setOpen(true);
        for (Channel channel : world.getChannels()) {
            MapleMap eventMap = channel.getMapFactory().getMap(MapId);
            eventMap.killAllMonsters();
            int[] ipos = PortalPositions[Randomizer.nextInt(PortalPositions.length)];
            Point pos = new Point(ipos[0], ipos[1]);
            MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);
            if (monster != null) {
                monster.addListener(new MonsterListener() {
                    @Override
                    public void monsterKilled(int aniTime) {
                        finished[channel.getId()] = true;
                    }
                });
                eventMap.spawnMonsterOnGroudBelow(monster, pos);
                channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The Space Slime has spawned in the Outer Space, Planet Lucian"));
            } else {
                System.err.println("Scheduled event 'Outer Space' was unable to spawn the monster " + MonsterId);
            }
        }
    }

    @Override
    public void end() {
        setOpen(false);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.saveLocation("OTHER");
        player.changeMap(98);
        player.announce(MaplePacketCreator.showEffect("event/boss/space"));
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        int returnMap = player.getSavedLocation("OTHER");
        if (returnMap == -1) {
            returnMap = 100000000;
        }
        player.changeMap(returnMap);
        player.clearSavedLocation(SavedLocationType.OTHER);
    }
}
