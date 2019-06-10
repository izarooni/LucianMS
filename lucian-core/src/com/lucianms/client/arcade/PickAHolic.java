package com.lucianms.client.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

public class PickAHolic extends Arcade {

    private int highscore = 0;
    private int prevScore = getHighscore(arcadeId, player);

    public PickAHolic(MapleCharacter player) {
        super(player);
        this.arcadeId = 0;
        this.mapId = 677000002;
        this.rewardPerKill = 0.2;
        this.itemReward = 4011024;

    }

    @Override
    public boolean nextRound() {
        return false;
    }

    @Override
    public boolean fail() {
        int itemId = 2100067;
        int quantity = player.getInventory(MapleInventoryType.USE).countById(itemId);

        if (quantity >= 1) {
            MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.USE, itemId, quantity, false, false);

            player.changeMap(978, 0);
            player.announce(MaplePacketCreator.serverNotice(1, "Game Over!"));
            if (saveData(highscore)) {
                player.dropMessage(5, "[Game Over] Your new highscore for Loot-A-Holic is " + highscore);
            } else {
                player.dropMessage(5, "[Game Over] Your highscore for Loot-A-Holic remains at " + Arcade.getHighscore(arcadeId, player));
            }

            // You know i had to do it to 'em
            for(int i =((int)(rewardPerKill * highscore)); i > 0; i--) {
                MapleInventoryManipulator.addById(player.getClient(), itemReward, (short) 1);
            }

            respawnTask = TaskExecutor.cancelTask(respawnTask);
            player.setArcade(null);
            return true;
        }
        return false;
    }

    @Override
    public void add() {
        int itemId = 4031544;
        int quantity = player.getInventory(MapleInventoryType.ETC).countById(itemId);

        if (quantity >= 1) {
            MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.ETC, itemId, quantity, false, false);
            ++highscore;
            player.announce(MaplePacketCreator.sendHint("#e[Loot-A-Holic]#n\r\nYou have looted " + ((prevScore < highscore) ? "#g" : "#r") + highscore + "#k card(s)!", 300, 40));
        }
    }

    @Override
    public void onKill(int monster) {}

    @Override
    public void onHit(int monster) {}

    @Override
    public boolean onBreak(int reactor) {
        return false;
    }

}
