package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BNormalMode extends BossPQ {

    public static final int[] bosses = new int[]{6130101, 6220000, 6400005, 6300005, 7120100, 7220000, 7220001, 7220002, 8180000, 8180001};

    private static final Point mSpawnPoint = new Point(0, -42);

    public BNormalMode(int channel) {
        super(channel, 270050100, bosses);
        setCashWinnings(7 * bosses.length);
        setPoints(25);
        setDamageMultiplier(1.0f);
        setHealthMultiplier(4.6f);
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
