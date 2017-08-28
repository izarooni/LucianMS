package server.events.custom.bpq;

import server.events.custom.BossPQ;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BEasyMode extends BossPQ {

    public static final int[] bosses = new int[]{4300013, 5220002, 5220000, 5220004, 9300039, 9300211, 9300212, 9300204};

    private static final Point mSpawnPoint = new Point(-391, -386);

    public BEasyMode(int channel) {
        super(channel, 800, bosses);
        setCashWinnings(5 * bosses.length);
        setPoints(15);
    }

    @Override
    public int getMinimumLevel() {
        return 60;
    }

    @Override
    public Point getMonsterSpawnPoint() {
        return mSpawnPoint;
    }
}
