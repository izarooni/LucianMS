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
package com.lucianms.server.maps;

import com.lucianms.client.MapleClient;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.io.scripting.reactor.ReactorScriptManager;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MobSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lerk
 * @author izarooni
 */
public class MapleReactor extends AbstractMapleMapObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleReactor.class);

    private MapleReactorStats stats;
    private String name;
    private int id;
    private int delay;
    private AtomicInteger state = new AtomicInteger(0);
    private boolean timerActive;
    private boolean alive = true;

    private Pair<MonsterStatus, MobSkill> status = null;
    private int team = -1;

    private volatile long lastHit = 0;

    private MapleMap map;

    public MapleReactor(MapleReactorStats stats, int id) {
        this.stats = stats;
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("MapleReactor{name='%s', id=%d, state=%s, alive=%s}", name, id, state.get(), alive);
    }

    public void setTimerActive(boolean active) {
        this.timerActive = active;
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    public void setState(byte state) {
        this.state.set(state);
    }

    public byte getState() {
        return (byte) state.get();
    }

    public int getId() {
        return id;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.REACTOR;
    }

    public int getReactorType() {
        return stats.getType((byte) state.get());
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public MapleMap getMap() {
        return map;
    }

    public Pair<Integer, Integer> getReactItem(byte index) {
        return stats.getReactItem((byte) state.get(), index);
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(makeDestroyData());
    }

    public final byte[] makeDestroyData() {
        return MaplePacketCreator.destroyReactor(this);
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(makeSpawnData());
    }

    public final byte[] makeSpawnData() {
        return MaplePacketCreator.spawnReactor(this);
    }

    public void forceHitReactor(final byte newState) {
        setState(newState);
        setTimerActive(false);
        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (short) 0));
    }

    public void delayedHitReactor(final MapleClient c, long delay) {
        TaskExecutor.createTask(() -> hitReactor(c), delay);
    }

    public void hitReactor(MapleClient client) {
        hitReactor(client, 0, (short) 0, 0);
    }

    public void hitReactor(MapleClient client, int charPos, short stance, int skillId) {
        final long elapsed = (System.currentTimeMillis() - lastHit);
        if (!isAlive() || elapsed < 700) {
            LOGGER.warn("Hitting reactor too frequently or reactor is destroyed ID:{}, elapsed_time:{}", id, elapsed);
            return;
        }
        final byte state = (byte) this.state.get();
        final int type = stats.getType(state);

        if (type < 999 && type != -1) {
            if (!(type == 2 && (charPos == 0 || charPos == 2))) { // get next state
                for (byte b = 0; b < stats.getStateSize(state); b++) {
                    final byte nextState = stats.getNextState(state, b);
                    List<Integer> activeSkills = stats.getActiveSkills(state, b);
                    if (activeSkills != null && !activeSkills.contains(skillId)) {
                        continue;
                    }
                    this.state.set(nextState);
                    if (stats.getNextState(nextState, b) == -1) {// end of reactor
                        if (type < 100) { // reactor broken
                            if (delay > 0) {
                                map.destroyReactor(getObjectId());
                            } else { // trigger as normal
                                map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                            }
                        } else { // item-triggered on final step
                            map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                        }
                        ReactorScriptManager.act(client, this);
                    } else { // reactor not broken yet
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                        if (state == nextState) {
                            ReactorScriptManager.act(client, this);
                        }
                    }
                    lastHit = System.currentTimeMillis();
                    break;
                }
            }
        } else {
            lastHit = System.currentTimeMillis();
            this.state.incrementAndGet();
            map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
            ReactorScriptManager.act(client, this);
        }
    }

    public Rectangle getArea() {
        return new Rectangle(getPosition().x + stats.getTL().x, getPosition().y + stats.getTL().y, stats.getBR().x - stats.getTL().x, stats.getBR().y - stats.getTL().y);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMonsterStatus(MonsterStatus status, MobSkill skill) {
        this.status = new Pair<>(status, skill);
    }

    public Pair<MonsterStatus, MobSkill> getMonsterStatus() {
        return status;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }
}
