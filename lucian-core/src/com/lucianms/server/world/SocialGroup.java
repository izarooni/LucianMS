package com.lucianms.server.world;

import com.lucianms.server.ConcurrentMapStorage;
import tools.PacketAnnouncer;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author izarooni
 */
public abstract class SocialGroup<E extends SocialMember> extends ConcurrentMapStorage<Integer, E> implements PacketAnnouncer {

    public SocialGroup(int initialSize) {
        super(initialSize);
    }

    public void forEachMember(Consumer<E> action) {
        values().stream().filter(s -> s.getPlayer() != null)
                .forEach(action);
    }

    public void forEachMember(Consumer<E> action, Predicate<E> condition) {
        values().stream().filter(s -> s.getPlayer() != null)
                .filter(condition)
                .forEach(action);
    }
}
