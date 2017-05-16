package server.events.custom.bpq;

import server.events.custom.BossPQ;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BNormalMode extends BossPQ {

    private static final int[] bosses = new int[]{8510000, 6090001, 6220000, 6300005, 8500001, 9300012, 9300028, 9300151, 9300206};

    public BNormalMode(int channel) {
        super(channel, 910050000, bosses);
        setCashWinnings(7 * bosses.length);
        setDamageMultiplier(2);
        setHealthMultiplier(2);
    }
}
