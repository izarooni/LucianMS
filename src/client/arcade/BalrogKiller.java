package client.arcade;

import client.MapleCharacter;
import client.MapleStat;
import server.MapleInventoryManipulator;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.MaplePacketCreator;

import java.awt.*;

public class BalrogKiller extends Arcade {


    private int highscore = 0;
    private int currExp = player.getExp();
    private int prevScore = getHighscore(arcadeId, player);


    public BalrogKiller(MapleCharacter player) {
        super(player);
        this.arcadeId = 4;
        this.mapId = 677000003;
        this.rewardPerKill = 0.9;
        this.itemReward = 4310149;
    }

    @Override
    public boolean fail() {

        player.setHp(player.getMaxHp());
        player.setExp(currExp);
        player.updateSingleStat(MapleStat.EXP, currExp);
        player.updateSingleStat(MapleStat.HP, player.getMaxHp());
        player.changeMap(970000000, 0);

        player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
        if (saveData(highscore)) {
            player.dropMessage(5, "[Game Over] Your new highscore for Balrog Killer is " + highscore);
        } else {
            player.dropMessage(5, "[Game Over] Your highscore for Balrog Killer remains at " + Arcade.getHighscore(arcadeId, player));
        }
        MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) (rewardPerKill * highscore));
        respawnTask.cancel();
        respawnTask = null;
        player.setArcade(null);
        return true;
    }

    @Override
    public void add() {
        currExp = player.getExp();
        ++highscore;
        player.announce(MaplePacketCreator.sendHint("#e[Balrog Killer]#n\r\nYou have killed " + ((prevScore < highscore) ? "#g" : "#r") + highscore + "#k balrog(s)!", 300, 40));
        MapleMonster toSpawn = MapleLifeFactory.getMonster(9500140);
        toSpawn.getStats().setMADamage(500);
        toSpawn.getStats().setPADamage(100);
        toSpawn.setHp(50000);
        player.getMap().spawnMonsterOnGroudBelow(toSpawn, new Point(206, 35));
    }

    @Override
    public void onKill(int monster) {
        int balrog = 9500140;
        if (monster == balrog) {
            add();
        }
    }

    @Override
    public void onHit(int monster) {
        player.setHp(player.getHp() - 5000);
        if (player.getHp() < 1) {
            fail();
        }

    }

    @Override
    public boolean onBreak(int reactor) {
        return false;
    }

}
