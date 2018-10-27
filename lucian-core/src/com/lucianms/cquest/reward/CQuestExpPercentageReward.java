package com.lucianms.cquest.reward;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ExpTable;

/**
 * @author izarooni
 */
public class CQuestExpPercentageReward implements CQuestReward {

    private final float exp;

    public CQuestExpPercentageReward(float exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "CQuestExpPercentageReward{" + "exp=" + exp + '}';
    }

    @Override
    public boolean canAccept(MapleCharacter player) {
        return true;
    }

    @Override
    public void give(MapleCharacter player) {
        player.gainExp(getExp(player), true, false);
    }

    public int getExp(MapleCharacter player) {
        return (int) (ExpTable.getExpNeededForLevel(player.getLevel()) * (this.exp / 100));
    }
}