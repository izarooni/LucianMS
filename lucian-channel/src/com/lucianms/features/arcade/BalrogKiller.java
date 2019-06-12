package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PlayerTakeDamageEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.awt.*;

/**
 * @author Lucasdieswagger
 * @author izarooni
 */
public class BalrogKiller extends Arcade {

    private static final int MapID = 677000003;
    private static final int RewardItemID = 4011024;
    private static final float RewardInc = 0.9f;

    private MapleMap map;
    private int score;
    private int highscore;
    private int calculatedDamage;

    public BalrogKiller() {
        registerAnnotationPacketEvents(this);
        this.arcadeID = 4;
    }

    @Override
    public void start() {
        MapleMonster monster = MapleLifeFactory.getMonster(9500140);
        assert monster != null;
        MapleMonsterStats overrides = new MapleMonsterStats(monster.getStats());
        overrides.setHp((calculatedDamage * 10));
        monster.getListeners().add(new MonsterListener() {
            @Override
            public void monsterKilled(MapleMonster monster, MapleCharacter player) {
                score += 1;
                player.announce(MaplePacketCreator.sendHint("#e[Balrog Killer]#n\r\nYou have killed " + ((highscore < score) ? "#g" : "#r") + score + "#k balrog(s)!", 300, 40));
                if (score % 5 == 0) {
                    start(); // let it get harder!
                }
                start();
            }
        });
        monster.setOverrideStats(overrides);
        map.spawnMonsterOnGroudBelow(monster, new Point(206, 35));
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        map = getMap(player.getClient(), MapID, null);
        map.toggleDrops();
        if (map == null) {
            player.sendMessage(1, "There is an internal problem with this Arcade game.");
            return;
        }
        calculatedDamage = player.calculateMaxBaseDamage(player.getTotalWatk());
        highscore = getHighscore(player.getId(), arcadeID);
        map.setReturnMapId(player.getMapId());
        player.changeMap(map);
        player.addGenericEvent(this);
        TaskExecutor.createTask(this::start, 5000);
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
            player.sendMessage(5, "[Game Over] Your new high-score for Balrog killer is {}", score);
        } else {
            player.sendMessage(5, "[Game Over] Your high-score for Balrog Killer remains at {}", highscore);
        }
        player.sendMessage(1, "Game over!");
        for (int i = ((int) (RewardInc * score)); i > 0; i--) {
            MapleInventoryManipulator.addById(player.getClient(), RewardItemID, (short) 1);
        }
    }

    @PacketWorker
    public void onPlayerHurt(PlayerTakeDamageEvent event) {
        final MapleCharacter player = event.getClient().getPlayer();
        event.setDamage(player.getMaxHp() / 15);
    }

    @Override
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        unregisterPlayer(player);
        return true;
    }
}
