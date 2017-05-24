package scripting.event;

import client.MapleCharacter;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import scripting.ScriptUtil;
import server.events.custom.GenericEvent;
import server.expeditions.MapleExpedition;
import server.maps.MapleMap;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author izarooni
 */
public class EventManager extends GenericEvent {

    private final Channel channel;
    private final String scriptName;
    private Invocable invocable;

    private ConcurrentHashMap<String, EventInstanceManager> instances = new ConcurrentHashMap<>();
    private Properties props = new Properties();

    public EventManager(Channel channel, String scriptName) {
        this.channel = channel;
        this.scriptName = scriptName;

        try {
            invocable = ScriptUtil.eval(null, "event/" + scriptName + ".js", Collections.singletonList(new Pair<>("em", this)));
        } catch (IOException | ScriptException e) {
            System.err.println(String.format("Unable to eval script '%s'", scriptName));
            e.printStackTrace();
        }
    }

    public Channel getChannel() {
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
        } catch (ScriptException | NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public Task schedule(String function, long delay) {
        return schedule(function, null, delay);
    }

    public Task schedule(String function, EventInstanceManager eim, long delay) {
        return createTask(new Runnable() {
            public void run() {
                try {
                    getInvocable().invokeFunction(function, eim);
                } catch (ScriptException | NoSuchMethodException e) {
                    System.err.println(String.format("Unable to invoke function '%s' in script '%s'", function, getScriptName()));
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    public Task schedule(String function, long delay, Object... args) {
        return createTask(new Runnable() {
            @Override
            public void run() {
                try {
                    getInvocable().invokeFunction(function, args);
                } catch (ScriptException | NoSuchMethodException e) {
                    System.err.println(String.format("Unable to invoke function '%s' in script '%s'", function, getScriptName()));
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    public Task scheduleAtTimestamp(final String function, long timestamp) {
        return createTask(new Runnable() {
            public void run() {
                try {
                    getInvocable().invokeFunction(function, (Object) null);
                } catch (ScriptException | NoSuchMethodException e) {
                    System.err.println(String.format("Unable to invoke function '%s' in script '%s'", function, getScriptName()));
                    e.printStackTrace();
                }
            }
        }, timestamp - System.currentTimeMillis());
    }

    public EventInstanceManager getInstance(String name) {
        return instances.get(name);
    }

    public Collection<EventInstanceManager> getInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public EventInstanceManager newInstance(String name) {
        if (instances.containsKey(name)) {
            throw new RuntimeException(String.format("Could not create new event instance with name(%s) already used for event(%s)", name, scriptName));
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
     * @param exped the expedition to initialize
     */
    public void startInstance(MapleExpedition exped) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerExpedition(exped);
            exped.start();
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Setup an event instance for a single player
     *
     * @param player the player initializing the event
     */
    public void startInstance(MapleCharacter player) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerPlayer(player);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Setup an event instance for a party
     *
     * @param party the party starting the party quest
     * @param map   the map containing all the party members
     */
    public void startInstance(MapleParty party, MapleMap map) {
        try {
            EventInstanceManager eim = (EventInstanceManager) (getInvocable().invokeFunction("setup", (Object) null));
            eim.registerParty(party, map);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startInstance(EventInstanceManager eim, String leader) {
        try {
            getInvocable().invokeFunction("setup", eim);
            eim.setProperty("leader", leader);
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(EventManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
