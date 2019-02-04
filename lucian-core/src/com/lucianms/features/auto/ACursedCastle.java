package com.lucianms.features.auto;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.*;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.StringUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * The objective of this event is to not get hit by the spawn monsters until there is only one player left
 * </p>
 *
 * @author izarooni
 */
public class ACursedCastle extends GAutoEvent {

    private static final int EventMap = 82;
    private static final int MonsterId = 9895244;
    private static final int SpawnQuantity = 25;
    private static final int xLow = 0, xHigh = 50, yLow = 0, yHigh = 50;

    private static long startTimestamp = 0L;

    private ConcurrentHashMap<Integer, Long> hits = new ConcurrentHashMap<>();
    private HashMap<Integer, Integer> returnMaps = new HashMap<>();

    public ACursedCastle(MapleWorld world) {
        super(world, true);
    }

    @Override
    public void start() {
        System.out.println("Be Careful! will begin momentarily");
        startTimestamp = System.currentTimeMillis();
        TaskExecutor.createTask(this::summonMonsters, 60000);
    }

    @Override
    public void stop() {
        GAutoEventManager.setCurrentEvent(null);
        for (MapleCharacter players : getPlayers()) {
            unregisterPlayer(players);
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        Long timestamp = hits.get(player.getId());
        if (timestamp != null) {
            player.dropMessage(5, String.format("You were disqualified from this event %s after it started", StringUtil.getTimeElapse(timestamp - startTimestamp)));
        } else {
            returnMaps.put(player.getId(), player.getMapId());
            player.changeMap(getMapInstance(EventMap));
            player.dropMessage("Welcome to Be Careful!");
            long endTimestamp = (startTimestamp + 60000);
            long timeLeft = (endTimestamp - System.currentTimeMillis());
            if (timeLeft > 0) {
                player.announce(MaplePacketCreator.getClock((int) (timeLeft / 1000)));
            }
        }
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.removeGenericEvent(this);
        if (returnMaps.containsKey(player.getId())) {
            int returnMap = returnMaps.remove(player.getId());
            player.changeMap(returnMap);
        }
    }

    @PacketWorker
    public void onCloseRangeAttack(PlayerDealDamageNearbyEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
        event.setCanceled(true);
    }

    @PacketWorker
    public void onRangedAttack(PlayerDealDamageRangedEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
        event.setCanceled(true);
    }

    @PacketWorker
    public void onMagicAttack(PlayerDealDamageMagicEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
        event.setCanceled(true);
    }

    @PacketWorker
    public void onPlayerTakeDamage(PlayerTakeDamageEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        unregisterPlayer(player);
        hits.put(player.getId(), System.currentTimeMillis()); // timestamp when the player got hit
        MapleCharacter winner = getWinner(player);

        if (winner != null) {
            // give reward
            winner.dropMessage("Congrats on winning! Here's your reward");
            winner.gainMeso(1, true);

            stop();
        }
        event.setCanceled(true);
    }

    @PacketWorker
    public void onPlayerDisconnect(MapleCharacter player) {
        unregisterPlayer(player);
        hits.put(player.getId(), System.currentTimeMillis()); // timestamp when the player got hit
        MapleCharacter winner = getWinner(player);

        if (winner != null) {
            // give reward
            winner.dropMessage("Congrats on winning! Here's your reward");
            winner.gainMeso(1, true);

            stop();
        }
    }

    /**
     * Monsters are supposed to be 2-shot kill
     * <p>
     * Damage will vary depending on player stats and shit so just cut the monster health by half
     * the first time then kill it the second time
     * </p>
     *
     * @param player     the player attack a monster
     * @param attackInfo the attack information
     */
    private void onMonsterAttacked(MapleCharacter player, AbstractDealDamageEvent.AttackInfo attackInfo) {
        MapleMap eventMap = getMapInstance(EventMap);
        for (Map.Entry<Integer, List<Integer>> entry : attackInfo.allDamage.entrySet()) {
            MapleMapObject object = eventMap.getMapObject(entry.getKey());
            if (object != null && object instanceof MapleMonster) {
                MapleMonster monster = (MapleMonster) object;
                if (monster.getHp() > (monster.getHp() / 2)) {
                    monster.setHp((int) Math.floor(monster.getHp() / 2d) - 1); // subtract 1 just to make sure HP is below half
                } else {
                    eventMap.killMonster(monster, player, false);
                }
            }
        }
    }

    private void summonMonsters() {
        MapleMap eventMap = getMapInstance(EventMap);
        int toSpawn = SpawnQuantity - eventMap.countMonster(MonsterId);
        if (toSpawn > 0) {
            for (int i = 0; i < toSpawn; i++) {
                MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);
                if (monster != null) {
                    MapleMonsterStats stats = new MapleMonsterStats();
                    stats.setHp(Integer.MAX_VALUE);
                    stats.setExp(0);
                    stats.setMp(0);
                    monster.setOverrideStats(stats);
                    Point pos = new Point(Randomizer.rand(xLow, xHigh), Randomizer.rand(yLow, yHigh));
                    eventMap.spawnMonsterOnGroudBelow(monster, pos);
                } else {
                    broadcastWorldMessage("Be Careful! is being canceled due to an error");
                    stop();
                    break;
                }
            }
        }
    }

    /**
     * Using the specified argument to compare with opponents, find the winner of the event
     *
     * @param hurt the player being disqualified
     * @return the winner, or null if there is no winner
     */
    private MapleCharacter getWinner(MapleCharacter hurt) {
        if (countPlayers() == 2) { // the other player is the winner!
            for (MapleCharacter players : getPlayers()) {
                if (players.getId() != hurt.getId()) {
                    return players;
                }
            }
            return null;
        } else if (countPlayers() > 2) { // there are still more players to disqualify
            return null;
        }
        // 1 player left? might be bugged if it wasn't caught by the first condition or disconnection; just declare this player as the winner
        return hurt;
    }
}
