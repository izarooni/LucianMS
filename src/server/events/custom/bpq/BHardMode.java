package server.events.custom.bpq;

import server.events.custom.BossPQ;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHardMode extends BossPQ {

    public static final int[] bosses = new int[]{7220003, 8220011, 8220010, 8800000, 8810000, 8810001, 8810003, 9001010, 9001009, 9300158};

    private static final Point pSpawnPoint = new Point(-407, -315);
    private static final Point mSpawnPoint = new Point(-323, 210);

    public BHardMode(int channel) {
        super(channel, 90000402, bosses);
        setCashWinnings(12 * bosses.length);
        setDamageMultiplier(4);
        setHealthMultiplier(4);
        setPoints(25);
    }

    @Override
    public int getMinimumLevel() {
        return 100;
    }

    @Override
    public Point getPlayerSpawnPoint() {
        return pSpawnPoint;
    }

    @Override
    public Point getMonsterSpawnPoint() {
        return mSpawnPoint;
    }
}
