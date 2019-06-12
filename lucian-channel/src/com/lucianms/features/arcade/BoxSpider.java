package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PlayerTakeDamageEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import tools.Randomizer;

import java.awt.*;

/**
 * @author Lucasdieswagger
 * @author izarooni
 */
public class BoxSpider extends Arcade {

    private static final int MonsterSpiderID = 2230103;
    private static final int MonsterBoxID = 9500365;
    private static final int MapID = 130000120;
    private static final float RewardInc = 0.15f;
    private static final int RewardItemID = 4011024;

    private static final int[] Platforms = new int[]{-1880, -2208, -2177, -1849};
    private static final int XPositionLeft = -2845, XPositionRight = -1603;

    private MapleMap map;
    private int score;
    private int highscore;

    public BoxSpider() {
        registerAnnotationPacketEvents(this);
        this.arcadeID = 2;
    }

    @Override
    public void start() {
        int yPos = Platforms[Randomizer.nextInt(Platforms.length)];
        int xPos = Randomizer.rand(XPositionLeft, XPositionRight);
        Point point = new Point(xPos, yPos);
        MapleMonster monster = MapleLifeFactory.getMonster(MonsterBoxID);
        assert monster != null;
        monster.getListeners().add(new MonsterListener() {
            @Override
            public void monsterKilled(MapleMonster monster, MapleCharacter player) {
                MapleMonster spider = MapleLifeFactory.getMonster(MonsterSpiderID);
                assert spider != null;
                MapleMonsterStats overrides = new MapleMonsterStats(monster.getStats());
                overrides.setHp(Long.MAX_VALUE);
                spider.setOverrideStats(overrides);
                map.spawnMonsterOnGroudBelow(spider, monster.getPosition());
                score++;
                start();
            }
        });
        MapleMonsterStats overrides = new MapleMonsterStats(monster.getStats());
        overrides.setHp(4);
        monster.setOverrideStats(overrides);
        map.spawnMonsterOnGroudBelow(monster, point);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        map = getMap(player.getClient(), MapID, null);
        map.toggleDrops();
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
        if (score > highscore) {
            saveData(player.getId(), score);
            player.sendMessage(5, "[Game Over] Your new high-score for Box Spider is {}", score);
        } else {
            player.sendMessage(5, "[Game Over] Your high-score for Box Spider remains at {}", highscore);
        }
        player.sendMessage(1, "Game over!");
        for (int i = ((int) (RewardInc * score)); i > 0; i--) {
            MapleInventoryManipulator.addById(player.getClient(), RewardItemID, (short) 1);
        }
    }

    @PacketWorker
    public void onPlayerHit(PlayerTakeDamageEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        if (event.getMonsterIdFrom() == MonsterSpiderID) {
            unregisterPlayer(player);
            event.setCanceled(true);
        }
    }
}