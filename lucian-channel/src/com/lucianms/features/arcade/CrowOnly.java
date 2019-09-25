package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.*;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import tools.MaplePacketCreator;

/**
 * @author Lucasdieswagger
 * @author izarooni
 */
public class CrowOnly extends Arcade {

    private static final int MonsterID = 9400000;
    private static final int MapID = 677000008;
    private static final float RewardInc = 0.1f;
    private static final int RewardItemID = 4011024;

    private MapleMap map;
    private int score;
    private int highscore;
    private MonsterListener monsterListener;

    public CrowOnly() {
        registerAnnotationPacketEvents(this);
        this.arcadeID = 3;

        monsterListener = new MonsterListener() {
            @Override
            public void monsterKilled(MapleMonster monster, MapleCharacter player) {
                if (monster.getId() == MonsterID) {
                    score += 1;
                    player.announce(MaplePacketCreator.sendHint("#e[Crow only]#n\r\nYou have killed " + ((highscore < score) ? "#g" : "#r") + score + "#k crow(s)!", 300, 40));
                }
            }
        };
    }

    @Override
    public void start() {
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        map = getMap(player.getClient(), MapID, FieldBuilder::loadMonsters);
        map.toggleDrops();
        if (map == null) {
            player.sendMessage(1, "There is an internal problem with this Arcade game.");
            return;
        }
        respawnTask = TaskExecutor.createRepeatingTask(map::respawn, 3, 5000);
        highscore = getHighscore(player.getId(), arcadeID);
        map.setReturnMapId(player.getMapId());
        player.changeMap(map);
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        respawnTask = TaskExecutor.cancelTask(respawnTask);
        player.removeGenericEvent(this);
        if (player.getMap().isInstanced()) {
            player.changeMap(player.getMap().getReturnMapId());
        }
        if (score > highscore) {
            saveData(player.getId(), score);
            player.sendMessage(5, "[Game Over] Your new high-score for Crow Only is {}", score);
        } else {
            player.sendMessage(5, "[Game Over] Your high-score for Crow Only remains at {}", highscore);
        }
        player.sendMessage(1, "Game over!");
        for (int i = ((int) (RewardInc * score)); i > 0; i--) {
            MapleInventoryManipulator.addById(player.getClient(), RewardItemID, (short) 1);
        }
    }

    @PacketWorker
    public void onCloseRangeAttack(PlayerDealDamageNearbyEvent event) {
        onMonsterHit(event);
    }

    @PacketWorker
    public void onFarRangeAttack(PlayerDealDamageRangedEvent event) {
        onMonsterHit(event);
    }

    @PacketWorker
    public void onMagicAttack(PlayerDealDamageMagicEvent event) {
        onMonsterHit(event);
    }

    private void onMonsterHit(AbstractDealDamageEvent event) {
        AbstractDealDamageEvent.AttackInfo attack = event.getAttackInfo();
        if (attack == null) {
            return;
        }
        for (Integer OID : attack.allDamage.keySet()) {
            MapleMonster monster = map.getMonsterByOid(OID);
            if (monster != null && monster.isAlive()) {
                if (monster.getId() == MonsterID) {
                    monster.getListeners().add(monsterListener);
                }
            }
        }
    }

    @PacketWorker
    public void onPlayerHit(PlayerTakeDamageEvent event) {
        if (event.getMonsterIdFrom() == MonsterID) {
            unregisterPlayer(event.getClient().getPlayer());
            event.setCanceled(true);
        }
    }
}
