package server.life;

import com.lucianms.scheduler.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapleMap;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SpawnPoint {

    public interface Summon {
        void summon();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnPoint.class);

    private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private final MapleMap map;
    private final MapleMonsterStats stats;
    private final Point pos;
    private final int monsterID;
    private final int mobTime, team, fh, f;
    private final boolean immobile;

    private int maximumSpawns = 1;
    private MapleMonster monster = null;
    private MapleMonsterStats overrides = null;
    private long nextPossibleSpawn;

    /**
     * @param monster  the monster associated with a SpawnPoint
     * @param immobile ?
     * @param mobTime  additional delay time before the next summon
     * @param team     a belonging team
     */
    public SpawnPoint(MapleMap map, MapleMonster monster, boolean immobile, int mobTime, int team) {
        this.map = map;
        this.stats = monster.getStats();
        this.monsterID = monster.getId();
        Point nPos = map.calcPointBelow(monster.getPosition());
        this.pos = (nPos == null) ? monster.getPosition() : nPos.getLocation();
        this.mobTime = (mobTime <= 0) ? 8 : mobTime;
        this.team = team;
        this.f = monster.getF();
        this.fh = monster.getFh();
        this.nextPossibleSpawn = System.currentTimeMillis();
        this.immobile = immobile;
    }

    public MapleMonsterStats createOverrides() {
        return overrides = new MapleMonsterStats(stats);
    }

    public boolean canSpawn(boolean force) {
        return map.isRespawnEnabled()
                && (!immobile || spawnedMonsters.get() == 0)
                && (spawnedMonsters.get() < maximumSpawns && (nextPossibleSpawn <= System.currentTimeMillis() || force));
    }

    /**
     * <p>Create and apply data to a newly created monster</p>
     * <p>This is to allow manual spawning of the monster and/or manually applying specific data to the monster before spawning</p>
     *
     * @return the instantiated monster
     */
    public MapleMonster getMonster() {
        monster = MapleLifeFactory.getMonster(monsterID);
        if (monster == null) {
            LOGGER.error("Unable to spawn monster (non-existing) {}", monsterID);
            return null;
        }
        monster.setPosition(pos.getLocation());
        monster.setTeam(team);
        monster.setFh(fh);
        monster.setF(f);
        if (overrides != null) {
            monster.setOverrideStats(overrides);
        }
        monster.getListeners().add(new MonsterListener() {
            @Override
            public void monsterKilled(int animationTime) {
                if (spawnedMonsters.get() > 0) {
                    spawnedMonsters.decrementAndGet();
                }
                nextPossibleSpawn = System.currentTimeMillis();
                nextPossibleSpawn += (mobTime > 0) ? (mobTime * 1000) : animationTime;

                long delay = Math.max(3000, (nextPossibleSpawn - System.currentTimeMillis()));
                attemptMonsterSummon(delay);
            }
        });
        return monster;
    }

    private void attemptMonsterSummon(long delay) {
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                if (canSpawn(false)) {
                    getMonster();
                    summonMonster();
                } else {
                    attemptMonsterSummon(delay);
                }
            }
        }, delay);
    }

    public void summonMonster() {
        if (monster == null) {
            LOGGER.error("Can't spawn monster (was never created)", getMonsterID());
            return;
        }
        spawnedMonsters.incrementAndGet();
        map.spawnMonsterOnGroudBelow(monster, monster.getPosition());
        monster = null;
    }

    public int getMaximumSpawns() {
        return maximumSpawns;
    }

    public void setMaximumSpawns(int maximumSpawns) {
        this.maximumSpawns = maximumSpawns;
    }

    public int getMonsterID() {
        return monsterID;
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
