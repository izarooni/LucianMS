package com.lucianms.io.scripting.event;

import com.lucianms.client.MapleCharacter;
import com.lucianms.features.GenericEvent;
import com.lucianms.io.scripting.ScriptUtil;
import com.lucianms.lang.DuplicateEntryException;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author izarooni
 */
public class EventManager extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    private final MapleChannel channel;
    private final String scriptName;
    private Invocable invocable;

    private HashMap<String, EventInstanceManager> instances = new HashMap<>();
    private Properties props = new Properties();

    public EventManager(MapleChannel channel, String scriptName) {
        this.channel = channel;
        this.scriptName = scriptName;

        try {
            invocable = ScriptUtil.eval("event/" + scriptName + ".js", Collections.singletonList(new Pair<>("em", this)));
        } catch (Exception e) {
            LOGGER.error("Failed to compile script '{}'", scriptName, e);
        }
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        throw new UnsupportedOperationException("The EventManager is not an event itself for players to be registered in");
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        throw new UnsupportedOperationException("The EventManager is not an event itself for players to be registered in");
    }

    public MapleChannel getChannel() {
        return channel;
    }

    public String getScriptName() {
        return scriptName;
    }

    public Invocable getInvocable() {
        return invocable;
    }

    public void cancel() {
        try {
            getInvocable().invokeFunction("cancelSchedule", (Object) null);
        } catch (NoSuchMethodException ignore) {
        } catch (ScriptException e) {
            LOGGER.error("Unable to invoke function cancelSchedule in script {}", scriptName, e);
        }
    }

    public Task schedule(String function, long delay) {
        return schedule(function, null, delay);
    }

    public Task schedule(String function, EventInstanceManager eim, long delay) {
        return TaskExecutor.createTask(new Runnable() {
            public void run() {
                try {
                    getInvocable().invokeFunction(function, eim);
                } catch (ScriptException | NoSuchMethodException e) {
                    LOGGER.error("Unable to invoke function {} in script {}", function, scriptName, e);
                }
            }
        }, delay);
    }

    public Task schedule(String function, long delay, Object... args) {
        return TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                try {
                    getInvocable().invokeFunction(function, args);
                } catch (ScriptException | NoSuchMethodException e) {
                    LOGGER.error("Unable to invoke function with varargs {} in script {}", function, scriptName, e);
                }
            }
        }, delay);
    }

    public Task scheduleAtTimestamp(final String function, long timestamp) {
        return TaskExecutor.createTask(new Runnable() {
            public void run() {
                try {
                    getInvocable().invokeFunction(function, (Object) null);
                } catch (ScriptException | NoSuchMethodException e) {
                    LOGGER.error("Unable to invoke function at timestamp {} in script {}", function, scriptName, e);
                }
            }
        }, timestamp - System.currentTimeMillis());
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return instances.values();
    }

    public EventInstanceManager newInstance(String name) {
        if (instances.containsKey(name)) {
            throw new DuplicateEntryException(String.format("Could not create new event instance with name(%s) for event '%s'", name, scriptName));
        }
        EventInstanceManager ret = new EventInstanceManager(this, name);
        instances.put(name, ret);
        return ret;
    }

    public void removeInstance(String name) {
        instances.remove(name);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public void setProperty(String key, Object value) {
        props.setProperty(key, value.toString());
    }

    /**
     * Setup an event instance for an expedition
     *
     * @param expedition the expedition to initialize
     */
    public void startInstance(MapleExpedition expedition) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerExpedition(expedition);
            expedition.start();
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to start maple expedition {}", expedition.getType().name(), e);
        }
    }

    /**
     * Setup an event instance for a single player
     *
     * @param player the player initializing the event
     */
    public EventInstanceManager startInstance(MapleCharacter player) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerPlayer(player);
            return eim;
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to start event instance {} for a single player", scriptName, e);
            return null;
        }
    }

    /**
     * Setup an event instance for a party
     *
     * @param party the party starting the party quest
     * @param map   the map containing all the party members
     */
    public EventInstanceManager startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
            return eim;
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to start event instance {} for party", scriptName, e);
            return null;
        }
    }

    public void startInstance(EventInstanceManager eim, String leader) {
        try {
            getInvocable().invokeFunction("setup", eim);
            eim.setProperty("leader", leader);
        } catch (ScriptException | NoSuchMethodException e) {
            LOGGER.error("Unable to invoke setup function of event {}", scriptName, e);
        }
    }
}
