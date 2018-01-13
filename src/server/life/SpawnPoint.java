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
package server.life;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpawnPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnPoint.class);

    private int monster, mobTime, team, fh, f;
    private Point pos;
    private long nextPossibleSpawn;
    private int mobInterval = 5000;
    private AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private boolean immobile;

    /**
     *
     * @param monster the monster associated with a SpawnPoint
     * @param pos position the specified monster will be summoned
     * @param immobile ?
     * @param mobTime additional delay time before the next summon
     * @param mobInterval the delay between each summon
     * @param team a belonging team
     */
    public SpawnPoint(final MapleMonster monster, Point pos, boolean immobile, int mobTime, int mobInterval, int team) {
        this.monster = monster.getId();
        this.fh = monster.getFh();
        this.f = monster.getF();

        this.pos = (pos == null) ? monster.getPosition() : pos.getLocation();
        this.immobile = immobile;
        this.mobTime = mobTime;
        this.mobInterval = mobInterval;
        this.team = team;
        this.nextPossibleSpawn = System.currentTimeMillis();
    }

    public boolean shouldSpawn() {
        return mobTime >= 0 && ((mobTime == 0 && !immobile) || spawnedMonsters.get() <= 0) && spawnedMonsters.get() <= 2 && nextPossibleSpawn <= System.currentTimeMillis();

    }

    public MapleMonster getMonster() {
        MapleMonster mob = MapleLifeFactory.getMonster(monster);
        if (mob == null) {
            return null;
        }
        mob.setPosition(pos.getLocation());
        mob.setTeam(team);
        mob.setFh(fh);
        mob.setF(f);
        spawnedMonsters.incrementAndGet();
        mob.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(int aniTime) {
                nextPossibleSpawn = System.currentTimeMillis();
                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime * 1000;
                } else {
                    nextPossibleSpawn += aniTime;
                }
                if (spawnedMonsters.get() > 0) {
                    // i have everything
                    spawnedMonsters.decrementAndGet();
                }
            }
        });
        if (mobTime == 0) {
            nextPossibleSpawn = System.currentTimeMillis() + mobInterval;
        }
        return mob;
    }

    public Point getPosition() {
        return pos;
    }

    public final int getF() {
        return f;
    }

    public final int getFh() {
        return fh;
    }

    public int getTeam() {
        return team;
    }
}
