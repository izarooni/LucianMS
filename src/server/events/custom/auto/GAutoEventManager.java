package server.events.custom.auto;

import net.server.world.World;
import tools.Randomizer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * @author izarooni
 */
public enum GAutoEventManager {

    FlappyBird("Flappy Bird", AFlappyBird.class),
    HeartlessWall("Heartless Wall", AHeartlessWall.class),
    WhispyWoods("Whispy Woods", AWhispyWoods.class),
    BeCareful("Be Careful!", ABeCareful.class);
    private final String name;
    private final Class<? extends GAutoEvent> clazz;
    private long lastInstantiate = 0L;

    private static GAutoEvent currentEvent;

    GAutoEventManager(String name, Class<? extends GAutoEvent> clazz) {
        this.name = name;
        this.clazz = clazz;
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

    public static GAutoEvent getCurrentEvent() {
        return currentEvent;
    }

    public static void setCurrentEvent(GAutoEvent currentEvent) {
        GAutoEventManager.currentEvent = currentEvent;
    }

    public static void startRandomEvent(World world) {
        final int interval = ((1000 * 60) * 60) * 2;
        GAutoEventManager[] events = GAutoEventManager.values();
        ArrayList<GAutoEventManager> available = new ArrayList<>();

        for (GAutoEventManager event : events) {
            if (System.currentTimeMillis() - event.getLastInstantiate() >= interval) {
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
