package com.lucianms.features.arcade;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.AbstractDealDamageEvent;
import com.lucianms.events.PlayerDealDamageMagicEvent;
import com.lucianms.events.PlayerDealDamageNearbyEvent;
import com.lucianms.events.PlayerDealDamageRangedEvent;
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
public class BobOnly extends Arcade {

    private static final int BobMonsterID = 9400551;
    private static final int MapID = 910100000;
    private static final float RewardInc = 0.4f;
    private static final int RewardItemID = 4011024;

    private int score;
    private int highscore;
    private MapleMap map;
    private MonsterListener monsterListener;

    public BobOnly() {
        registerAnnotationPacketEvents(this);
        this.arcadeID = 1;

        monsterListener = new MonsterListener() {
            @Override
            public void monsterKilled(MapleMonster monster, MapleCharacter player) {
                score += 1;
                player.announce(MaplePacketCreator.sendHint("#e[Bob only]#n\r\nYou have killed " + ((highscore < score) ? "#g" : "#r") + score + "#k bob(s)!", 300, 40));
            }
        };
    }

    @Override
    public void start() {
        respawnTask = TaskExecutor.createRepeatingTask(new Runnable() {
            @Override
            public void run() {
                if (map.getCharacters().isEmpty()) {
                    respawnTask = TaskExecutor.cancelTask(respawnTask);
                    map.respawn();
                }
            }
        }, 5000);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        map = getMap(player.getClient(), MapID, FieldBuilder::loadMonsters);
        map.toggleDrops();
        if (map == null) {
            player.sendMessage(1, "There is an internal problem with this Arcade game.");
            return;
        }
        respawnTask = TaskExecutor.createRepeatingTask(map::respawn, 0, 5000);
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
        player.sendMessage(1, "Game over!");
        if (score > highscore) {
            saveData(player.getId(), score);
            player.sendMessage(5, "[Game Over] Your new high-score for Bob Only is {}", score);
        } else {
            player.sendMessage(5, "[Game Over] Your high-score for Bob Only remains at {}", highscore);
        }
        // You know i had to do it to 'em
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
        MapleCharacter player = event.getClient().getPlayer();
        for (Integer OID : attack.allDamage.keySet()) {
            MapleMonster monster = map.getMonsterByOid(OID);
            if (monster != null && monster.isAlive()) {
                if (monster.getId() == BobMonsterID) {
                    monster.getStats().getRevives().clear();
                    monster.getListeners().add(monsterListener);
                } else {
                    unregisterPlayer(player);
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
