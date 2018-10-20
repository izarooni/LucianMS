package com.lucianms.io.scripting.event;

import com.lucianms.server.channel.MapleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class EventScriptManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventScriptManager.class);

    private final MapleChannel channel;
    private ConcurrentHashMap<String, EventManager> events = new ConcurrentHashMap<>();

    public EventScriptManager(MapleChannel channel) {
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
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ScriptException e) {
                LOGGER.error("Unable to invoke 'init' script '{}'", manager.getScriptName(), e.getStackTrace()[0]);
            }
        }
    }

    public void close() {
        events.values().forEach(EventManager::cancel);
    }
}