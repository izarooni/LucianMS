package com.lucianms.features.auto;

import net.server.world.World;
import tools.Randomizer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Does not record each event last instantiate for multiple worlds simply because there is no need to
 *
 * @author izarooni
 */
public enum GAutoEventManager {

    // @formatter:off
    FlappyBird("Flappy Bird",             AFlappyBird.class, false, 0),
    HeartlessWall("Heartless Wall",    AHeartlessWall.class, false, 0),
    WhispyWoods("Whispy Woods",          AWhispyWoods.class, false, 0),
    CursedCastle("Cursed Castle",       ACursedCastle.class, false, 0),
    EmergencyTrial("Emergency Trial", AEmergencyTrial.class,  true, 3);
    // @formatter:on
    private final String name;
    private final Class<? extends GAutoEvent> clazz;
    private final boolean enabled;
    private long lastInstantiate = 0L;
    public long interval = 3;

    private static GAutoEvent currentEvent;

    GAutoEventManager(String name, Class<? extends GAutoEvent> clazz, boolean enabled, long interval) {
        this.name = name;
        this.clazz = clazz;
        this.enabled = enabled;
        this.interval = interval;
    }

    public String getName() {
        return name;
    }

    public Class<? extends GAutoEvent> getClazz() {
        return clazz;
    }

    public long getLastInstantiate() {
        return lastInstantiate;
    }

    public void setLastInstantiate(long lastInstantiate) {
        this.lastInstantiate = lastInstantiate;
    }

    public void startInstance(World world) {
        try {
            GAutoEvent gEvent = clazz.getDeclaredConstructor(World.class, boolean.class).newInstance(world, false);
            gEvent.start();
            setCurrentEvent(gEvent);
            setLastInstantiate(System.currentTimeMillis());
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static GAutoEvent getCurrentEvent() {
        return currentEvent;
    }

    public static void setCurrentEvent(GAutoEvent currentEvent) {
        GAutoEventManager.currentEvent = currentEvent;
    }

    /**
     * Pick a random event that {@link #getLastInstantiate()} was at least 2 hours ago
     * <p>
     * The selected child auto event class will instantiate using the {@code World} method parameter.
     * The last instantiate will be set and {@link GAutoEvent#start()} will be invoked
     * </p>
     *
     * @param world the world the auto event will be hosted in
     */
    public static void startRandomEvent(World world) {
        if (getCurrentEvent() != null) {
            // maybe implement a force cancel function
            getCurrentEvent().stop();
        }
        GAutoEventManager[] events = GAutoEventManager.values();
        ArrayList<GAutoEventManager> available = new ArrayList<>();

        for (GAutoEventManager event : events) {
            long interval = ((1000 * 60) * 60) * event.interval;
            if (event.enabled && System.currentTimeMillis() - event.getLastInstantiate() >= interval) {
                available.add(event);
            }
        }

        try {
            if (!available.isEmpty()) {
                GAutoEventManager event = null;
                while (event == null) {
                    event = available.get(Randomizer.nextInt(available.size()));
                    try {
                        GAutoEvent gEvent = event.clazz.getDeclaredConstructor(World.class).newInstance(world);
                        gEvent.start();
                        setCurrentEvent(gEvent);
                        event.setLastInstantiate(System.currentTimeMillis());
                    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            available.clear();
        }
    }
}
