package com.lucianms.features.bpq;

import client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHardMode extends BossPQ {

    public static final int[] bosses = new int[]{7220005, 8220011, 8220010, 8800002, 8810000, 8810001, 9400518, 9001010, 9001009, 9300158};

    private static final Point mSpawnPoint = new Point(-391, -386);

    public BHardMode(int channel) {
        super(channel, 802, bosses);
        setCashWinnings(12 * bosses.length);
        setDamageMultiplier(2.2f);
        setHealthMultiplier(2.2f);
        setPoints(25);
    }

    @Override
    public int getMinimumLevel() {
        return 100;
    }

    @Override
    public Point getMonsterSpawnPoint() {
        return mSpawnPoint;
    }

    @Override
    public void giveRewards(MapleCharacter player) {
        player.addPoints("ep", 3);
        player.dropMessage("You gained 3 event point and now have a total of " + player.getEventPoints());
    }
}
