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
    private final int monsterID;
    private final int mobTime, team, fh, f;

    private MapleMonster monster = null;
    private MapleMonsterStats overrides = null;
    private Point pos;
    private long nextPossibleSpawn;
    private boolean immobile;

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
        this.mobTime = (mobTime <= 0) ? 5 : mobTime;
        this.team = team;
        this.f = monster.getF();
        this.fh = monster.getFh();
        this.nextPossibleSpawn = System.currentTimeMillis();
        this.immobile = immobile;
    }

    public MapleMonsterStats createOverrides() {
        return overrides = new MapleMonsterStats(stats);
    }

    public boolean shouldSpawn() {
        return ((mobTime == 0 && !immobile) // instant spawn must be a monster capable of movement
                || spawnedMonsters.get() < 0) // otherwise make sure no monsters have already been spawned
                && (spawnedMonsters.get() <= 2 && nextPossibleSpawn <= System.currentTimeMillis());
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
        monster.addListener(new MonsterListener() {
            @Override
            public void monsterKilled(int aniTime) {
                if (spawnedMonsters.get() > 0) {
                    spawnedMonsters.decrementAndGet();
                }
                nextPossibleSpawn = System.currentTimeMillis();
                if (mobTime > 0) {
                    nextPossibleSpawn += mobTime * 1000;
                } else {
                    nextPossibleSpawn += aniTime;
                }

                long delay = Math.max(0, (nextPossibleSpawn - System.currentTimeMillis()));
                TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        getMonster();
                        summonMonster();
                    }
                }, delay);
                LOGGER.info("Summon monster {} in {}ms", getMonsterID(), delay);
            }
        });

        return monster;
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
