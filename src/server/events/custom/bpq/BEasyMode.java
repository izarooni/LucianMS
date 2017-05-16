package server.events.custom.bpq;

import server.events.custom.BossPQ;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BEasyMode extends BossPQ {

    private static final int[] bosses = new int[]{4300013, 5220002, 5220000, 5220004, 9300039, 9300211, 9300212, 9300204};

    public BEasyMode(int channel) {
        super(channel, 910050000, bosses);
        setCashWinnings(5 * bosses.length);
        setPoints(15);
    }
}
