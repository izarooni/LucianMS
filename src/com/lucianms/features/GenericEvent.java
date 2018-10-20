package com.lucianms.features;

import client.MapleCharacter;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.events.PacketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapleMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author izarooni
 */
public abstract class GenericEvent {

    private HashMap<Class<?>, ArrayList<Method>> methods = new HashMap<>();
    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * Iterates through methods of the specified object class and stores
     * found valid methods in an array list which is mapped to a packet event class
     *
     * @param object a class that contains annotated methods for packet event handlers
     */
    protected final void registerAnnotationPacketEvents(Object object) {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(PacketWorker.class) != null && method.getParameterCount() == 1) {
                Class<?>[] pTypes = method.getParameterTypes();
                // only add methods which parameters meet specifications
                // in this case, there is 1 parameter and it is a child of the PacketEvent
                if (PacketEvent.class.isAssignableFrom(pTypes[0])) {
                    methods.putIfAbsent(pTypes[0], new ArrayList<>());
                    methods.get(pTypes[0]).add(method);
//                    LOGGER.info("Registered method {}", method.getName());
                }
            }
        }
    }

    public final Logger logger() {
        return LOGGER;
    }

    /**
     * Invokes all methods stored with a matching packet event parameter to this specified method call parameter.
     * <p>
     * Not to be overridden by a child class
     * </p>
     *
     * @param event a packet event
     */
    public final void onPacketEvent(PacketEvent event) {
        if (methods.get(event.getClass()) != null) {
            methods.get(event.getClass()).forEach(method -> {
                try {
//                    LOGGER.info("PacketEvent method '{}({})' invoked", method.getName(), event.getClass().getSimpleName());
                    method.invoke(this, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * When the player change's map via method call
     *
     * @param player      the player changing maps
     * @param destination the destination map
     * @return true if the player can continue transfer to new map, false otherwise
     */
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        return true;
    }

    /**
     * When the player dies
     *
     * @param player the player dying
     * @return true if the death is being handled
     */
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        // do nothing
        return false;
    }

    /**
     * When the client is disconnecting
     *
     * @param player the player of the disconnecting client
     */
    public void onPlayerDisconnect(MapleCharacter player) {
        // do nothing
    }

    /**
     * When the player is being 'banished' (warped away) due to a monster skill
     *
     * @param player the player changing maps
     * @param mapId  the map the player will be banished to
     * @return true if the player should be banished
     */
    public boolean banishPlayer(MapleCharacter player, int mapId) {
        // do nothing
        return true;
    }

    public abstract void registerPlayer(MapleCharacter player);

    public abstract void unregisterPlayer(MapleCharacter player);
}
