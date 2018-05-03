package com.lucianms.features;

import client.MapleCharacter;
import com.lucianms.server.events.channel.ChangeMapEvent;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.SpawnPoint;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import com.lucianms.lang.annotation.PacketWorker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author izarooni
 */
public class EmergencyAttack extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyAttack.class);

    private ArrayList<Integer> players = new ArrayList<>();
    private AtomicInteger summoned = null;
    private boolean canceled = false;
    private MapleMap map = null;
    private Task task = null;
    private int totalExp = 0;

    public EmergencyAttack() {
        registerAnnotationPacketEvents(this);
    }

    @PacketWorker
    public void onMapChange(ChangeMapEvent event) {
        task.cancel();
        new Timeout(false).run();
        unregisterPlayers(event.getClient().getWorldServer());
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        Optional<GenericEvent> first = player.getGenericEvents().stream().filter(g -> g instanceof EmergencyAttack).findFirst();
        if (first.isPresent()) {
            canceled = true;
            LOGGER.info("Player '{}' triggered but already in existing emergency instance", player.getName());
            return;
        }
        player.addGenericEvent(this);
        players.add(player.getId());
        int avgLevel;
        map = player.getMap();
        map.killAllMonsters();
        map.setRespawnEnabled(false);

        int delay = 120;
        MapleParty party = player.getParty();
        Collection<MaplePartyCharacter> members;
        if (party != null && (members = party.getMembers()).size() > 2) {
            delay = 60;
            avgLevel = 0;
            for (MaplePartyCharacter member : members) {
                avgLevel += member.getLevel();
            }
            avgLevel /= members.size();
        } else {
            avgLevel = player.getLevel();
        }
        task = TaskExecutor.createTask(new Timeout(true), 1000 * delay);

        map.broadcastMessage(MaplePacketCreator.getClock(delay));
        summonMonsters(player.getClient().getWorldServer(), avgLevel);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    private void summonMonsters(final World world, int level) {
        SpawnPoint[] sp = map.getMonsterSpawnPoints().toArray(new SpawnPoint[0]);
        for (int i = 0; i < monsters.length; i++) {
            int[] stats = monsters[i];
            int[] next = (i == (monsters.length - 1)) ? null : monsters[i + 1];
            if (next == null || (level >= stats[0] && level <= next[0])) {
                final int summons = Math.min(30, sp.length);
                summoned = new AtomicInteger(summons);
                for (int j = 0; j < summons; j++) {
                    MapleMonster monster = MapleLifeFactory.getMonster(stats[1]);
                    if (monster != null) {
                        monster.addListener((ani) -> monsterDeath(world, monster));
                        map.spawnMonsterOnGroudBelow(monster, sp[Randomizer.nextInt(sp.length)].getPosition());
                    } else {
                        summoned.decrementAndGet();
                    }
                }
                map.broadcastMessage(MaplePacketCreator.earnTitleMessage(String.format("%s monsters at level %s have appeared!", summons, stats[0])));
                map.broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/3"));
                map.broadcastMessage(MaplePacketCreator.playSound("PSO2/Attack"));
                break;
            }
        }
    }

    private void monsterDeath(World world, MapleMonster monster) {
        totalExp += monster.getExp();

        int remaining = summoned.decrementAndGet();
        if (remaining == 0) {
            task.cancel();
            totalExp *= 1.50;
            unregisterPlayers(world);
            map.setRespawnEnabled(true);
            map.broadcastMessage(MaplePacketCreator.removeClock());
            map.broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/5"));
            map.broadcastMessage(MaplePacketCreator.playSound("PSO2/Completed"));
            for (Integer integer : players) {
                MapleCharacter player = world.getPlayerStorage().getCharacterById(integer);
                if (player != null) {
                    player.gainExp(totalExp, true, true);
                }
            }
        } else {
            map.broadcastMessage(MaplePacketCreator.serverNotice(5, "[Emergency] There are " + remaining + " monsters left."));
        }
    }

    private void unregisterPlayers(World world) {
        for (Integer integer : players) {
            MapleCharacter player = world.getPlayerStorage().getCharacterById(integer);
            if (player != null) {
                unregisterPlayer(player);
            }
        }
    }

    private class Timeout implements Runnable {

        private final boolean effect;

        Timeout(boolean effect) {
            this.effect = effect;
        }

        @Override
        public void run() {
            map.killAllMonsters();
            map.setRespawnEnabled(true);
            if (effect) {
                map.broadcastMessage(MaplePacketCreator.showEffect("dojang/timeOver"));
            }
            task = null;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    private static final int[][] monsters = new int[][]{
            {20, 9400595}, {25, 2100103}, {30, 3000005}, {35, 3230200},
            {40, 9400517}, {45, 4130100}, {50, 5120503}, {55, 5130101},
            {60, 9420511}, {65, 9420534}, {70, 9400640}, {75, 7130200},
            {80, 9400545}, {85, 7130010}, {90, 8140700}, {95, 9895239},
            {100, 9895240}, {105, 8200005}, {110, 8190003}, {115, 8200008},
            {120, 8200009}, {125, 8200011}, {130, 8200012}, {135, 8600000},
            //{140, }, {145, },
            {150, 9400112}, {155, 9400113},
            {160, 8642003},
            //{165, },
            {170, 8610006},
            //{175, },
            //{180, }, {185, },
            {190, 8620000}, {195, 8620007},
            {200, 8620009},
    };
}
