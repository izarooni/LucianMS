package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BNormalMode extends BossPQ {

    public static final int[] bosses = new int[]{8510000, 6090001, 6220000, 6300005, 8500001, 9300012, 9300028, 9300151, 9300206};

    private static final Point mSpawnPoint = new Point(-391, -386);

    public BNormalMode(int channel) {
        super(channel, 801, bosses);
        setCashWinnings(7 * bosses.length);
        setPoints(25);
        setDamageMultiplier(1.0f);
        setHealthMultiplier(1.0f);
    }

    @Override
    public int getMinimumLevel() {
        return 80;
    }

    @Override
    public Point getMonsterSpawnPoint() {
        return mSpawnPoint;
    }

    @Override
    public void giveRewards(MapleCharacter player) {
        player.addPoints("ep", 2);
        player.dropMessage("You gained 2 event point and now have a total of " + player.getEventPoints());
    }
}
