package com.lucianms.io.scripting.event;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.ConcurrentMapStorage;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/**
 * @author izarooni
 */
public class EventInstanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventInstanceManager.class);

    private final EventManager eventManager;
    private final String name;
    private final Properties props = new Properties();

    private ConcurrentMapStorage<Integer, MapleCharacter> concurrentMapStorage = new ConcurrentMapStorage<>();
    private HashMap<Integer, MapleMap> maps = new HashMap<>(15);
    private HashMap<Integer, MapleMonster> monsters = new HashMap<>(); // <ObjectID, Monster>
    private Map<Integer, Integer> killCount = new HashMap<>(); // <PlayerID, Count>
    private ArrayList<Integer> tasks = new ArrayList<>(); // <TaskID>

    private long timeStarted = 0;
    private long eventTime = 0;
    private MapleExpedition expedition = null;

    public EventInstanceManager(EventManager eventManager, String name) {
        this.eventManager = eventManager;
        this.name = name;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Task schedule(final String function, long delay) {
        Task task = eventManager.schedule(function, this, delay);
        tasks.add(task.getId());
        return task;
    }

    public void registerPlayer(MapleCharacter player) {
        if (player == null || !player.isLoggedin()) {
            LOGGER.warn("Unable to register null player into event instance '{}'", getName());
            return;
        }
        try {
            concurrentMapStorage.put(player.getId(), player);
            player.setEventInstance(this);
            eventManager.getInvocable().invokeFunction("playerEntry", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to register player '{}' into event instance '{}'", player.getName(), getName(), e);
        }
    }

    public void registerParty(MapleParty party, MapleMap map) {
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter c = map.getCharacterById(pc.getId());
            registerPlayer(c);
        }
    }

    public void registerExpedition(MapleExpedition exped) {
        expedition = exped;
        registerPlayer(exped.getLeader());
    }

    public void unregisterPlayer(MapleCharacter player) {
        player.setEventInstance(null);
        concurrentMapStorage.remove(player.getId());
    }

    public int getPlayerCount() {
        return concurrentMapStorage.size();
    }

    public Collection<MapleCharacter> getPlayers() {
        return concurrentMapStorage.values();
    }

    public void registerMonster(MapleMonster monster) {
        if (!monster.getStats().isFriendly()) { //We cannot register moon bunny
            monsters.put(monster.getObjectId(), monster);
            monster.setEventInstance(this);
        }
    }

    public boolean movePlayer(MapleCharacter player, MapleMap map) {
        try {
            return (boolean) eventManager.getInvocable().invokeFunction("moveMap", this, player, map);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'moveMap' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
        return true;
    }

    public void monsterKilled(MapleMonster monster) {
        monsters.remove(monster.getObjectId());
        if (monsters.isEmpty()) {
            try {
                eventManager.getInvocable().invokeFunction("allMonstersDead", this);
            } catch (ScriptException | NoSuchMethodException e) {
                LOGGER.error("Unable to invoke function 'allMonstersDead' with monster '{}' in event instance '{}'", monster.getId(), getName(), e);
            }
        }
    }

    public void playerKilled(MapleCharacter player) {
        try {
            eventManager.getInvocable().invokeFunction("playerDead", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'playerDead' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
    }

    public boolean revivePlayer(MapleCharacter player) {
        try {
            return (boolean) eventManager.getInvocable().invokeFunction("playerRevive", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'playerRevive' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
        return true;
    }

    public void playerDisconnected(MapleCharacter player) {
        try {
            eventManager.getInvocable().invokeFunction("playerDisconnected", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'playerDisconnected' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
    }

    public void monsterKilled(MapleCharacter player, MapleMonster monster) {
        try {
            Integer kc = killCount.getOrDefault(player.getId(), 0);
            kc += (int) eventManager.getInvocable().invokeFunction("monsterValue", this, player, monster);
            killCount.put(player.getId(), kc);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'monsterValue' with player '{}' and monster '{}' in event instance '{}'", player.getName(), monster.getId(), getName(), e);
        }
    }

    public int getKillCount(MapleCharacter player) {
        return killCount.getOrDefault(player.getId(), 0);
    }

    public void dispose() {
        try {
            eventManager.getInvocable().invokeFunction("dispose", this);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'dispose' in event instance '{}'", getName(), e);
        }

        concurrentMapStorage.values().forEach(this::removePlayer);
        concurrentMapStorage.clear();

        if (monsters != null) monsters.clear();
        monsters = null;

        if (killCount != null) killCount.clear();
        killCount = null;

        tasks.forEach(TaskExecutor::cancelTask);

        if (expedition != null) {
            eventManager.getChannel().getExpeditions().remove(expedition);
        }

        maps.clear();
    }

    public String getName() {
        return name;
    }

    public void saveWinner(MapleCharacter chr) {
        try (Connection con = eventManager.getChannel().getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, eventManager.getScriptName());
            ps.setString(2, getName());
            ps.setInt(3, chr.getId());
            ps.setInt(4, chr.getClient().getChannel());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MapleMap removeMapInstance(int mapID) {
        return maps.remove(mapID);
    }

    public MapleMap getMapInstance(int mapID, Function<FieldBuilder, MapleMap> consumer) {
        MapleMap map = maps.computeIfAbsent(mapID, id -> consumer.apply(new FieldBuilder(eventManager.getChannel().getWorld(), eventManager.getChannel().getId(), mapID)));
        if (eventManager.getProperty("shuffleReactors") != null && eventManager.getProperty("shuffleReactors").equals("true")) {
            map.shuffleReactors();
        }
        return map;
    }

    public MapleMap getMapInstance(int mapID) {
        MapleMap map = maps.computeIfAbsent(mapID, id -> new FieldBuilder(eventManager.getChannel().getWorld(), eventManager.getChannel().getId(), mapID).loadAll().build());
        if (eventManager.getProperty("shuffleReactors") != null && eventManager.getProperty("shuffleReactors").equals("true")) {
            map.shuffleReactors();
        }
        return map;
    }

    public void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public Properties getProperties() {
        return props;
    }

    public void leftParty(MapleCharacter player) {
        try {
            eventManager.getInvocable().invokeFunction("leftParty", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'leftParty' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
    }

    public void disbandParty() {
        try {
            eventManager.getInvocable().invokeFunction("disbandParty", this);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'disbandParty' in event instance '{}'", getName(), e);
        }
    }

    public void finishPQ() {
        try {
            eventManager.getInvocable().invokeFunction("clearPQ", this);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'clearPQ' in event instance '{}'", getName(), e);
        }
    }

    public void removePlayer(MapleCharacter player) {
        try {
            unregisterPlayer(player);
            eventManager.getInvocable().invokeFunction("playerExit", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'playerExit' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr.getParty().getLeader().getId() == chr.getId());
    }

    public void startEventTimer(long time) {
        timeStarted = System.currentTimeMillis();
        eventTime = time;
    }

    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }
}
