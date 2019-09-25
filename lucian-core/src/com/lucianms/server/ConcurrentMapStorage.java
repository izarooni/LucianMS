package com.lucianms.server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link java.util.concurrent.ConcurrentHashMap} :angry:
 *
 * @author izarooni
 */
public class ConcurrentMapStorage<K, V> {

    private final ReentrantReadWriteLock.ReadLock rLock;
    private final ReentrantReadWriteLock.WriteLock wLock;
    private final ConcurrentHashMap<K, V> storage;

    public ConcurrentMapStorage() {
        this(80);
    }

    public ConcurrentMapStorage(int initialSize) {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        rLock = lock.readLock();
        wLock = lock.writeLock();

        storage = new ConcurrentHashMap<>((int) (initialSize / 0.75) + 1);
    }

    public final void clear() {
        wLock.lock();
        try {
            storage.clear();
        } finally {
            wLock.unlock();
        }
    }

    public final void put(K key, V player) {
        wLock.lock();
        try {
            storage.put(key, player);
        } finally {
            wLock.unlock();
        }
    }

    public final V remove(K key) {
        wLock.lock();
        try {
            return storage.remove(key);
        } finally {
            wLock.unlock();
        }
    }

    public final void forEach(Consumer<V> consumer) {
        rLock.lock();
        try {
            storage.values().forEach(consumer);
        } finally {
            rLock.unlock();
        }
    }

    public final V find(Predicate<V> predicate) {
        rLock.lock();
        try {
            return storage.values().stream().filter(predicate).findFirst().orElse(null);
        } finally {
            rLock.unlock();
        }
    }

    public boolean containsKey(K key) {
        rLock.lock();
        try {
            return storage.containsKey(key);
        } finally {
            rLock.unlock();
        }
    }

    public final V get(K key) {
        rLock.lock();
        try {
            return storage.get(key);
        } finally {
            rLock.unlock();
        }
    }

    public final Collection<V> values() {
        rLock.lock();
        try {
            return storage.values();
        } finally {
            rLock.unlock();
        }
    }

    public final int size() {
        rLock.lock();
        try {
            return storage.size();
        } finally {
            rLock.unlock();
        }
    }

    public final boolean isEmpty() {
        rLock.lock();
        try {
            return storage.isEmpty();
        } finally {
            rLock.unlock();
        }
    }

    public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        wLock.lock();
        try {
            return storage.computeIfAbsent(key, mappingFunction);
        } finally {
            wLock.unlock();
        }
    }
}