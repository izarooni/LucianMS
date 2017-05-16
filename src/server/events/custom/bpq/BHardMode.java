package server.events.custom.bpq;

import server.events.custom.BossPQ;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHardMode extends BossPQ {

    private static final int[] bosses = new int[]{7220003, 8220011, 8220010, 8800000, 8810000, 8810001, 8810003, 9001010, 9001009, 9300158};

    public BHardMode(int channel) {
        super(channel, 910050000, bosses);
        setCashWinnings(12 * bosses.length);
        setDamageMultiplier(4);
        setHealthMultiplier(4);
    }
}
