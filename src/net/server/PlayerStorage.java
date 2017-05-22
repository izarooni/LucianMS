/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server;

import client.MapleCharacter;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PlayerStorage {

    private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();
    private final Lock rlock = locks.readLock();
    private final Lock wlock = locks.writeLock();
    private final Map<Integer, MapleCharacter> storage = new LinkedHashMap<>();

    public void clear() {
        storage.clear();
    }

    public void addPlayer(MapleCharacter player) {
        wlock.lock();
        try {
            storage.put(player.getId(), player);
        } finally {
            wlock.unlock();
        }
    }

    public MapleCharacter removePlayer(int playerID) {
        wlock.lock();
        try {
            return storage.remove(playerID);
        } finally {
            wlock.unlock();
        }
    }

    public MapleCharacter getCharacterByName(String name) {
        rlock.lock();
        try {
            for (MapleCharacter player : storage.values()) {
                if (player.getName().toLowerCase().equals(name.toLowerCase())) {
                    return player;
                }
            }
            return null;
        } finally {
            rlock.unlock();
        }
    }

    public MapleCharacter getCharacterById(int playerID) {
        rlock.lock();
        try {
            return storage.get(playerID);
        } finally {
            rlock.unlock();
        }
    }

    public Collection<MapleCharacter> getAllCharacters() {
        rlock.lock();
        try {
            return new ArrayList<>(storage.values());
        } finally {
            rlock.unlock();
        }
    }

    public void disconnectAll() {
        wlock.lock();
        try {
            final Iterator<MapleCharacter> iter = storage.values().iterator();
            while (iter.hasNext()) {
                iter.next().getClient().disconnect(true, false);
                iter.remove();
            }
        } finally {
            wlock.unlock();
        }
    }

    public int size() {
        rlock.lock();
        try {
            return storage.size();
        } finally {
            rlock.unlock();
        }
    }
}