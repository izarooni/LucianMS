package client.arcade;

import client.MapleCharacter;
import server.MapleInventoryManipulator;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

public class BoxSpider extends Arcade {

    private int[] platform;

    int xStart = 2624, xEnd = 619;


    private int highscore = 0;
    private int monsterId = 2230103;
    private int box = 9500365;
    private int prevScore = getHighscore(arcadeId, player);

    private boolean touched = false;

    public BoxSpider(MapleCharacter player) {
        super(player);
        this.mapId = 130000101;
        this.arcadeId = 2;
        this.platform = new int[]{88, -210, -569, -897, -1224};
        this.rewardPerKill = 0.15;
        this.itemReward = 4310149;

    }

    @Override
    public boolean fail() {
        if (touched) {

            player.changeMap(970000000, 0);
            player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
            if (saveData(highscore)) {
                player.dropMessage(5, "[Game Over] Your new highscore for Box Spider is " + highscore);
            } else {
                player.dropMessage(5, "[Game Over] Your highscore for Box Spider remains at " + Arcade.getHighscore(arcadeId, player));
            }
        }
        MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) (rewardPerKill * highscore));
        respawnTask.cancel();
        respawnTask = null;
        player.setArcade(null);
        return true;
    }

    @Override
    public void add() {
        ++highscore;
        player.announce(MaplePacketCreator.sendHint("#e[Box Spider]#n\r\nYou have destroyed " + ((prevScore < highscore) ? "#g" : "#r") + highscore + "#k box(es)!", 300, 40));

        MapleMonster toSpawn = MapleLifeFactory.getMonster(box);
        if (toSpawn == null) {
            toSpawn = MapleLifeFactory.getMonster(9500365);
        }

        int spawnX = Randomizer.nextInt(2621 - 619) + 619;
        spawnX = (spawnX >= 619) ? spawnX : 619;
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
