package server.events.custom.bpq;

import server.events.custom.BossPQ;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BNormalMode extends BossPQ {

    public static final int[] bosses = new int[]{8510000, 6090001, 6220000, 6300005, 8500001, 9300012, 9300028, 9300151, 9300206};

    private static final Point pSpawnPoint = new Point(-28, 181);
    private static final Point mSpawnPoint = new Point(-28, 181);

    public BNormalMode(int channel) {
        super(channel, 910050000, bosses);
        setCashWinnings(7 * bosses.length);
        setPoints(25);
        setDamageMultiplier(2);
        setHealthMultiplier(2);
    }

    @Override
    public int getMinimumLevel() {
        return 80;
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
