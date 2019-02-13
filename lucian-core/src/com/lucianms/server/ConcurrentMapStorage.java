package com.lucianms.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link java.util.concurrent.ConcurrentHashMap} :angry:
 *
 * @author izarooni
 */
public final class ConcurrentMapStorage<K, V> {

    private final ReentrantReadWriteLock.ReadLock rLock;
    private final ReentrantReadWriteLock.WriteLock wLock;

    // initialize capacity of 80 objects using the default load factor value
    private final HashMap<K, V> storage = new HashMap<>((int) (80 / 0.75) + 1);

    public ConcurrentMapStorage() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        rLock = lock.readLock();
        wLock = lock.writeLock();
    }

    public void clear() {
        wLock.lock();
        try {
            storage.clear();
        } finally {
            wLock.unlock();
        }
    }

    public void put(K key, V player) {
        wLock.lock();
        try {
            storage.put(key, player);
        } finally {
            wLock.unlock();
        }
    }

    public V remove(K key) {
        wLock.lock();
        try {
            return storage.remove(key);
        } finally {
            wLock.unlock();
        }
    }

    public void forEach(Consumer<V> consumer) {
        rLock.lock();
        try {
            storage.values().forEach(consumer);
        } finally {
            rLock.unlock();
        }
    }

    public V find(Predicate<V> predicate) {
        rLock.lock();
        try {
            return storage.values().stream().filter(predicate).findFirst().orElse(null);
        } finally {
            rLock.unlock();
        }
    }

    public V get(K key) {
        rLock.lock();
        try {
            return storage.get(key);
        } finally {
            rLock.unlock();
        }
    }

    public Collection<V> values() {
        rLock.lock();
        try {
            return storage.values();
        } finally {
            rLock.unlock();
        }
    }

    public int size() {
        rLock.lock();
        try {
            return storage.size();
        } finally {
            rLock.unlock();
        }
    }

    public boolean isEmpty() {
        rLock.lock();
        try {
            return storage.isEmpty();
        } finally {
            rLock.unlock();
        }
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        wLock.lock();
        try {
            return storage.computeIfAbsent(key, mappingFunction);
        } finally {
            wLock.unlock();
        }
    }
}