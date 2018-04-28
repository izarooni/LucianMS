package client.arcade;

import client.MapleCharacter;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

public class MobRush extends Arcade {

    private int highscore = 0;
    private int prevScore = getHighscore(arcadeId, player);
    private Task task;

    private int stage = 1;

    public MobRush(MapleCharacter player) {
        super(player);
        this.mapId = 910330100;
        this.arcadeId = 5;
        this.rewardPerKill = 0.01;
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
        if(this.task != null) {
            this.task.cancel();
        }
        player.setArcade(null);
        return true;
    }

    @Override
    public boolean nextRound() {
        if(stage <= 3) {
            this.mapId += 1;
            this.stage += 1;

            player.getMap().getAllPlayer().forEach((target) -> target.changeMap(this.mapId));

            player.getMap().getPortals().forEach((portal) -> portal.setPortalStatus(false));
            player.getMap().setMobInterval((short) 5);

            // disable drops
            player.getMap().toggleDrops();

            player.getMap().broadcastMessage(MaplePacketCreator.showEffect("killing/first/start"));
           TaskExecutor.createTask(() ->  player.getMap().broadcastMessage(MaplePacketCreator.showEffect("killing/first/number/" + this.stage)), 3000);

            player.announce(MaplePacketCreator.getClock(180));
            this.task = TaskExecutor.createTask(this::nextRound, 180000);
        } else {
            player.getMap().broadcastMessage(MaplePacketCreator.showEffect("killing/clear"));
            TaskExecutor.createTask(this::fail, 3000); // Game over
        }

        return false;
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

        if(player.getMap().getMonsters().size() >= 60) {
            this.fail();
        }
    }

    @Override
    public boolean onBreak(int reactor) {
        return false;
    }

}
