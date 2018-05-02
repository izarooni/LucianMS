package com.lucianms.cquest.reward;

import client.MapleCharacter;

/**
 * @author izarooni
 */
public class CQuestExpReward implements CQuestReward {

    private final int exp;

    public CQuestExpReward(int exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "CQuestExpReward{" + "exp=" + exp + '}';
    }

    @Override
    public boolean canAccept(MapleCharacter player) {
        return true;
    }

    @Override
    public void give(MapleCharacter player) {
        player.gainExp(exp, true, false);
    }

    public int getExp() {
        return exp;
    }
}
