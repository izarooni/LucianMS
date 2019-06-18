package com.lucianms.features.bpq;

import com.lucianms.client.MapleCharacter;

import java.awt.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class BHellMode extends BossPQ {

    public static final int[] bosses = new int[]{8170000, 8220012, 8820001, 8820002, 9400518, 8820004, 8820005, 8820006, 8830000, 8830001, 8830002, 9001014, 9895253};

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
