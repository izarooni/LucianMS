package scripting.event;

import net.server.channel.Channel;
import scripting.ScriptUtil;
import tools.Pair;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class EventScriptManager {

    private final Channel channel;
    private ConcurrentHashMap<String, EventManager> events = new ConcurrentHashMap<>();

    public EventScriptManager(Channel channel) {
        this.channel = channel;
    }

    public EventManager getManager(String event) {
        return events.get(event);
    }

    public void putManager(String event) {
        EventManager em = new EventManager(channel, event);
        if (em.getInvocable() == null) {
            throw new RuntimeException(String.format("Unable to instantiate event manager '%s'", event));
        }
        events.put(event, em);
    }

    public void removeManager(String event) {
        if (events.containsKey(event)) {
            events.get(event).cancel();
        }
    }

    public void init() {
        for (EventManager manager : events.values()) {
            try {
                manager.getInvocable().invokeFunction("init", (Object) null);
            } catch (ScriptException | NoSuchMethodException e) {
                System.err.println(String.format("Unable to invoke init function in script '%s'", manager.getScriptName()));
                e.printStackTrace();
            }
        }
    }

    public void close()
    {
        events.values().forEach(EventManager::cancel);
    }
}