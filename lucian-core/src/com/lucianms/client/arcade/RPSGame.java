package com.lucianms.client.arcade;

/**
 * @author izarooni
 */
public class RPSGame {

    private int round = 0;
    private byte selection = -1;

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public byte getSelection() {
        return selection;
    }

    public void setSelection(byte selection) {
        this.selection = selection;
    }
}
