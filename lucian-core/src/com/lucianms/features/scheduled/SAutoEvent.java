package com.lucianms.features.scheduled;

import com.lucianms.features.GenericEvent;

/**
 * Represents the root class of a scheduled auto event
 *
 * @author izarooni
 */
public abstract class SAutoEvent extends GenericEvent {

    public abstract String getName();

    public abstract long getInterval();

    public abstract void run();

    public abstract void end();
}
