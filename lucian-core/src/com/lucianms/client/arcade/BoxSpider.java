package com.lucianms.client.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

public class BoxSpider extends Arcade {

    private int[] platform;

    int xStart = -2684, xEnd = -1803;


    private int highscore = 0;
    private int monsterId = 2230103;
    private int box = 9500365;
    private int prevScore = getHighscore(arcadeId, player);

    private boolean touched = false;

    public BoxSpider(MapleCharacter player) {
        super(player);
        this.mapId = 130000120;
        this.arcadeId = 2;
        this.platform = new int[]{-1888, -2208, -2177, -1849};
        this.rewardPerKill = 0.15;
        this.itemReward = 4011024;
    }

    @Override
    public boolean nextRound() {
        return false;
    }

    @Override
    public boolean fail() {
        if (touched) {
            player.setArcade(null);

            player.changeMap(978, 0);

            player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
            if (saveData(highscore)) {
                player.dropMessage(5, "[Game Over] Your new highscore for Box Spider is " + highscore);
            } else {
                player.dropMessage(5, "[Game Over] Your highscore for Box Spider remains at " + Arcade.getHighscore(arcadeId, player));
            }

            // You know i had to do it to 'em
            for (int i = ((int) (rewardPerKill * highscore)); i > 0; i--) {
                MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) 1);
            }

            respawnTask = TaskExecutor.cancelTask(respawnTask);
            return true;
        }
        return false;
    }

    @Override
    public void add() {
        ++highscore;
        player.announce(MaplePacketCreator.sendHint("#e[Box Spider]#n\r\nYou have destroyed " + ((prevScore < highscore) ? "#g" : "#r") + highscore + "#k box(es)!", 300, 40));

        MapleMonster toSpawn = MapleLifeFactory.getMonster(box);
        if (toSpawn == null) {
            toSpawn = MapleLifeFactory.getMonster(9500365);
        }

        int spawnX = Randomizer.rand((1803 - 2684), 2684);
        spawnX = (spawnX >= xStart && spawnX <= xEnd) ? spawnX : 619;
        int spawnY = Randomizer.nextInt(platform.length);

        toSpawn.setHp(4);
        Point p = new Point(-spawnX, platform[spawnY]);
        player.getMap().spawnMonsterOnGroudBelow(toSpawn, p);
    }

    @Override
    public void onKill(int monster) {
        if (monster == box) {
            add();
        }
    }

    @Override
    public void onHit(int monster) {
        if (monster == monsterId) {
            touched = true;
            fail();
        }

    }

    @Override
    public boolean onBreak(int reactor) {
        return false;
    }

}
