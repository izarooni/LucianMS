package server.events.custom.scheduled;

import server.events.custom.GenericEvent;

/**
 * Represents the root class of a scheduled auto event
 *
 * @author izarooni
 */
public abstract class SAutoEvent extends GenericEvent {

    public abstract String getName();

    public abstract long getInterval();

    public abstract void run();
}
