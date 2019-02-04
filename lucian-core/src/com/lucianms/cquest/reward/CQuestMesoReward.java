package com.lucianms.cquest.reward;

import com.lucianms.client.MapleCharacter;

/**
 * @author izarooni
 */
public class CQuestMesoReward implements CQuestReward {

    private final int meso;

    public CQuestMesoReward(int meso) {
        this.meso = meso;
    }

    @Override
    public String toString() {
        return "CQuestMesoReward{" + "meso=" + meso + '}';
    }

    @Override
    public boolean canAccept(MapleCharacter player) {
        // should overflow happen, a negative integer would be returned
        return player.getMeso() + meso > 0;
    }

    @Override
    public void give(MapleCharacter player) {
        player.gainMeso(meso, true, false, true);
    }

    public int getMeso() {
        return meso;
    }
}
