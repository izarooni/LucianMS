package scripting.event;

import client.MapleCharacter;
import net.server.PlayerStorage;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.expeditions.MapleExpedition;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DatabaseConnection;

import javax.script.ScriptException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * @author izarooni
 */
public class EventInstanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventInstanceManager.class);

    private final EventManager eventManager;
    private final String name;
    private final Properties props = new Properties();

    private MapleMapFactory mapFactory;
    private PlayerStorage playerStorage = new PlayerStorage();
    private HashMap<Integer, MapleMonster> monsters = new HashMap<>(); // <ObjectID, Monster>
    private Map<Integer, Integer> killCount = new HashMap<>(); // <PlayerID, Count>
    private ArrayList<Integer> tasks = new ArrayList<>(); // <TaskID>

    private long timeStarted = 0;
    private long eventTime = 0;
    private MapleExpedition expedition = null;

    public EventInstanceManager(EventManager eventManager, String name) {
        this.eventManager = eventManager;
        this.name = name;

        mapFactory = new MapleMapFactory(eventManager.getChannel().getWorld(), eventManager.getChannel().getId());
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
            playerStorage.addPlayer(player);
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
        playerStorage.removePlayer(player.getId());
    }

    public int getPlayerCount() {
        return playerStorage.size();
    }

    public Collection<MapleCharacter> getPlayers() {
        return Collections.unmodifiableCollection(playerStorage.getAllCharacters());
    }

    public void registerMonster(MapleMonster monster) {
        if (!monster.getStats().isFriendly()) { //We cannot register moon bunny
            monsters.put(monster.getObjectId(), monster);
            monster.setEventInstance(this);
        }
    }

    public void movePlayer(MapleCharacter player) {
        try {
            eventManager.getInvocable().invokeFunction("moveMap", this, player);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke function 'moveMap' with player '{}' in event instance '{}'", player.getName(), getName(), e);
        }
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
            if (expedition != null) {
                expedition.monsterKilled(player, monster);
            }
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
        playerStorage.getAllCharacters().forEach(this::removePlayer);
        playerStorage.clear();

        monsters.clear();
        monsters = null;

        killCount.clear();
        killCount = null;

        tasks.forEach(TaskExecutor::cancelTask);

        eventManager.removeInstance(name);
        if (expedition != null) {
            eventManager.getChannel().getExpeditions().remove(expedition);
        }

        mapFactory.clear();
        mapFactory = null;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public String getName() {
        return name;
    }

    public void saveWinner(MapleCharacter chr) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO eventstats (event, instance, characterid, channel) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, eventManager.getScriptName());
            ps.setString(2, getName());
            ps.setInt(3, chr.getId());
            ps.setInt(4, chr.getClient().getChannel());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MapleMap getMapInstance(int mapId) {
        MapleMap map = mapFactory.getMap(mapId);
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
