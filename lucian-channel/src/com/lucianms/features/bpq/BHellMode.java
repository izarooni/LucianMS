package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHellMode extends BossPQ {

    public static final int[] bosses = new int[]{
            8641010, 9303206, 8840000, 8850111,
            8910000, 8910001, // Chaos Von Bon
            8920000, 8920001, 8920002, 8920003, // Chaos Crimson Queen
            9300823, 9300854, 9300855, 9303153, 9410560
    };

    private static final Point mSpawnPoint = new Point(0, -42);

    public BHellMode(int channel) {
        super(channel, 270050100, bosses);
        setCashWinnings(20 * bosses.length);
        setDamageMultiplier(3.0f);
        setHealthMultiplier(15f);
        setPoints(75);
    }

    @Override
    public int getMinimumLevel() {
        return 200;
    }

    @Override
    public Point getMonsterSpawnPoint() {
        return mSpawnPoint;
    }

    @Override
    public void giveRewards(MapleCharacter player) {
        player.addPoints("ep", 4);
        player.dropMessage("You gained 4 event point and now have a total of " + player.getEventPoints());
    }
}
