package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.ItemPickupEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.maps.MapleMapObject;
import tools.MaplePacketCreator;

/**
 * @author Lucasdieswagger
 * @author izarooni
 */
public class PickAHolic extends Arcade {

    private static final int MapID = 677000003;
    private static final int RewardItemID = 4011024;
    private static final float RewardInc = 0.2f;
    private static final int ItemID = 2100067;

    private MapleMap map;
    private int score;
    private int highscore;

    public PickAHolic() {
        registerAnnotationPacketEvents(this);
        this.arcadeID = 0;
    }

    @Override
    public void start() {

    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        map = getMap(player.getClient(), MapID, null);
        if (map == null) {
            player.sendMessage(1, "There is an internal problem with this Arcade game.");
            return;
        }
        highscore = getHighscore(player.getId(), arcadeID);
        map.setReturnMapId(player.getMapId());
        player.changeMap(map);
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        if (player.getMap().isInstanced()) {
            player.changeMap(player.getMap().getReturnMapId());
        }
        if (!player.isAlive()) {
            player.setHpMp(player.getMaxHp(), player.getMaxMp());
        }
        if (score > highscore) {
            saveData(player.getId(), score);
            player.sendMessage(5, "[Game Over] Your new high-score for Pick-A-Holic is {}", score);
        } else {
            player.sendMessage(5, "[Game Over] Your high-score for Pick-A-Holic remains at {}", highscore);
        }
        player.sendMessage(1, "Game over!");
        for (int i = ((int) (RewardInc * score)); i > 0; i--) {
            MapleInventoryManipulator.addById(player.getClient(), RewardItemID, (short) 1);
        }
    }

    @PacketWorker
    public void onPickupDrop(ItemPickupEvent event) {
        event.setCanceled(true);
        MapleCharacter player = event.getClient().getPlayer();
        MapleMapObject mapObject = map.getMapObject(event.getObjectId());
        if (!(mapObject instanceof MapleMapItem)) {
            return;
        }
        MapleMapItem drop = (MapleMapItem) mapObject;
        if (drop.getItemId() == ItemID) {
            score++;
            player.announce(MaplePacketCreator.sendHint("#e[Loot-A-Holic]#n\r\nYou have looted " + ((highscore < score) ? "#g" : "#r") + highscore + "#k card(s)!", 300, 40));
        } else {
            unregisterPlayer(player);
        }
    }
}
