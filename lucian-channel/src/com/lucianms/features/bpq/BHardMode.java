package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHardMode extends BossPQ {

    public static final int[] bosses = new int[]{8210003, 8210005, 8210010, 8220003, 8220006, 8500001, 8510000, 8620012, 8642011, 9421581};

    private static final Point mSpawnPoint = new Point(0, -42);

    public BHardMode(int channel) {
        super(channel, 270050100, bosses);
        setCashWinnings(12 * bosses.length);
        setDamageMultiplier(1.5f);
        setHealthMultiplier(8.6f);
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
