package client.arcade;

import client.MapleCharacter;
import scheduler.TaskExecutor;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

public class MobRush extends Arcade {

    private int highscore = 0;
    private int prevScore = getHighscore(arcadeId, player);

    public MobRush(MapleCharacter player) {
        super(player);
        this.mapId = 910330100;
        this.arcadeId = 5;
        this.rewardPerKill = 0.02;
        this.itemReward = 4011024;
    }

    @Override

    public boolean fail() {
        player.changeMap(978, 0);
        player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
        if (saveData(highscore)) {
            player.dropMessage(5, "[Game Over] Your new highscore for Mob Rush is " + highscore);
        } else {
            player.dropMessage(5, "[Game Over] Your highscore for Mob Rush remains at " + Arcade.getHighscore(arcadeId, player));
        }

        // You know i had to do it to 'em
        for(int i =((int)(rewardPerKill * highscore)); i > 0; i--) {
            MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) 1);
        }

        respawnTask.cancel();
        respawnTask = null;
        player.setArcade(null);
        return true;
    }

    @Override
    public void add() {
        ++highscore;
        player.announce(MaplePacketCreator.sendHint("#e[Mob rush]#n\r\nYou have killed " + ((prevScore < highscore) ? "#g" : "#r") + highscore + "#k monster(s)!", 300, 40));
    }

    @Override
    public void onKill(int monster) {
       add();
    }

    @Override
    public void onHit(int monster) {
        if (player.getHp() < 1) {
            fail();
        }

        // more than 90 = fail :rage:
        if(player.getMap().getMonsters().size() >= 90) {
            this.fail();
        }
    }

    @Override
    public boolean onBreak(int reactor) {
        return false;
    }

}
