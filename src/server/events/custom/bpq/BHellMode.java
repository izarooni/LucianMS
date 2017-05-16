package server.events.custom.bpq;

import server.events.custom.BossPQ;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHellMode extends BossPQ {

    private static final int[] bosses = new int[]{8170000, 8220012, 8820001, 8820002, 8820003, 8820004, 8820005, 8820006, 8830000, 8830001, 8830002, 9001014};

    public BHellMode(int channel) {
        super(channel, 910050000, bosses);
        setCashWinnings(20 * bosses.length);
        setDamageMultiplier(10);
        setHealthMultiplier(10);
    }
}
