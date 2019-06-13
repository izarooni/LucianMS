package com.lucianms.io.scripting.event;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import javax.script.ScriptException;
import java.util.*;
import java.util.function.Function;

/**
 * @author izarooni
 */
public class EventInstanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventInstanceManager.class);

    private final EventManager eventManager;
    private final String name;
    @Deprecated
    private final Properties props = new Properties();
    public ScriptObjectMirror vars;

    private HashMap<Integer, MapleMap> maps = new HashMap<>(15);
    private HashMap<Integer, MapleCharacter> players = new HashMap<>();
    private HashMap<Integer, MapleMonster> monsters = new HashMap<>(); // <ObjectID, Monster>
    private Map<Integer, Integer> killCount = new HashMap<>(); // <PlayerID, Count>
    private ArrayList<Task> tasks = new ArrayList<>(); // <TaskID>

    private long timeStarted = 0;
    private long eventTime = 0;
    private MapleExpedition expedition = null;

    public EventInstanceManager(EventManager eventManager, String name) {
        this.eventManager = eventManager;
        this.name = name;
    }

    public Object invokeFunction(String function, Object... args) {
        try {
            return eventManager.getInvocable().invokeFunction(function, args);
        } catch (NoSuchMethodException e) {
            LOGGER.warn("Missing function '{}' in event '{}'", function, eventManager.getScriptName());
        } catch (ScriptException e) {
            LOGGER.error("Event script {}", eventManager.getScriptName(), e);
        }
        return null;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void broadcastPacket(byte[] packet) {
        getPlayers().forEach(p -> p.announce(packet));
    }

    public void broadcastMessage(byte type, String content) {
        broadcastPacket(MaplePacketCreator.serverNotice(type, content));
    }

    public Task schedule(final String function, long delay) {
        Task task = eventManager.schedule(function, this, delay);
        tasks.add(task);
        return task;
    }

    public void registerPlayer(MapleCharacter player) {
        if (player == null) {
            LOGGER.warn("Unable to register null player into event instance '{}'", getName());
            return;
        }
        addPlayer(player);
        invokeFunction("playerEntry", this, player);
    }

    public void registerParty(MapleParty party, MapleMap map) {
        for (MaplePartyCharacter pc : party.values()) {
            MapleCharacter c = map.getCharacterById(pc.getPlayerID());
            registerPlayer(c);
        }
    }

    public void registerExpedition(MapleExpedition exped) {
        expedition = exped;
        registerPlayer(exped.getLeader());
    }

    public void addPlayer(MapleCharacter player) {
        player.setEventInstance(this);
        players.put(player.getId(), player);
    }

    public void unregisterPlayer(MapleCharacter player) {
        player.setEventInstance(null);
        players.remove(player.getId());
    }

    public boolean containsPlayer(int playerID) {
        return players.get(playerID) != null;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Collection<MapleCharacter> getPlayers() {
        return players.values();
    }

    public void registerMonster(MapleMonster monster) {
        if (!monster.getStats().isFriendly()) { //We cannot register moon bunny
            monsters.put(monster.getObjectId(), monster);
            monster.setEventInstance(this);
        }
    }

    public boolean movePlayer(MapleCharacter player, MapleMap map) {
        Object result = invokeFunction("moveMap", this, player, map);
        if (result == null) return true;
        return (boolean) result;
    }

    public void monsterKilled(MapleMonster monster) {
        monsters.remove(monster.getObjectId());
        if (monsters.isEmpty()) {
            invokeFunction("allMonstersDead", this);
        }
    }

    public void playerKilled(MapleCharacter player) {
        invokeFunction("playerDead", this, player);
    }

    public boolean revivePlayer(MapleCharacter player) {
        return (boolean) invokeFunction("playerRevive", this, player);
    }

    public void playerDisconnected(MapleCharacter player) {
        invokeFunction("playerDisconnected", this, player);
    }

    public void monsterKilled(MapleCharacter player, MapleMonster monster) {
        Integer kc = killCount.getOrDefault(player.getId(), 0);
        kc += (int) invokeFunction("monsterValue", this, player, monster);
        killCount.put(player.getId(), kc);
    }

    public int getKillCount(MapleCharacter player) {
        return killCount.getOrDefault(player.getId(), 0);
    }

    public void dispose() {
        invokeFunction("dispose", this);

        players.clear();

        if (monsters != null) monsters.clear();
        monsters = null;

        if (killCount != null) killCount.clear();
        killCount = null;

        tasks.forEach(TaskExecutor::cancelTask);

        if (expedition != null) {
            eventManager.getChannel().getExpeditions().remove(expedition);
        }

        maps.clear();
        props.clear();
    }

    public String getName() {
        return name;
    }

    public MapleMap removeMapInstance(int mapID) {
        return maps.remove(mapID);
    }

    public MapleMap getMapInstance(int mapID, Function<FieldBuilder, MapleMap> consumer) {
        MapleMap map = maps.computeIfAbsent(mapID, id -> consumer.apply(new FieldBuilder(eventManager.getChannel().getWorld(), eventManager.getChannel().getId(), mapID)));
        if (eventManager.getProperty("shuffleReactors") != null && eventManager.getProperty("shuffleReactors").equals("true")) {
            map.shuffleReactors();
        }
        map.setInstanced(true);
        return map;
    }

    public MapleMap getMapInstance(int mapID) {
        MapleMap map = maps.computeIfAbsent(mapID, id -> new FieldBuilder(eventManager.getChannel().getWorld(), eventManager.getChannel().getId(), mapID).loadAll().build());
        if (eventManager.getProperty("shuffleReactors") != null && eventManager.getProperty("shuffleReactors").equals("true")) {
            map.shuffleReactors();
        }
        map.setInstanced(true);
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
        invokeFunction("leftParty", this, player);
    }

    public void disbandParty() {
        invokeFunction("disbandParty", this);
    }

    public void finishPQ() {
        invokeFunction("clearPQ", this);
    }

    public void removePlayer(MapleCharacter player) {
        unregisterPlayer(player);
        invokeFunction("playerExit", this, player);
    }

    public boolean isLeader(MapleCharacter chr) {
        return (chr.getParty().getLeader().getPlayerID() == chr.getId());
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
