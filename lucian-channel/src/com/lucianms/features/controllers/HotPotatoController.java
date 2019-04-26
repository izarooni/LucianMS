package com.lucianms.features.controllers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleDisease;
import com.lucianms.events.PlayerMoveEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.life.MobSkillFactory;
import com.lucianms.server.maps.MapleMap;
import tools.Disposable;
import tools.Duplicable;
import tools.MaplePacketCreator;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author izarooni
 */
public class HotPotatoController extends GenericEvent implements Disposable {

    private static final int PotatoMonsterID = 8500003;
    private MapleMonster potatoMonster;
    private MapleMap map;
    private int potatoHolder;
    private long lastSwap;
    private Task timeoutTask;

    public HotPotatoController() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void dispose() {
        end();
    }

    public void start() {
        map.sendPacket(MaplePacketCreator.getClock(180));
        timeoutTask = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                end();
            }
        }, TimeUnit.MINUTES.toMillis(3));
    }

    public void end() {
        timeoutTask = TaskExecutor.cancelTask(timeoutTask);
        MapleCharacter holder = map.getCharacterById(potatoHolder);
        if (holder != null) {
            holder.setHpMp(0);
            unregisterPlayer(holder);
            map.sendMessage(5, "{} has lost the game of Hot Potato!", holder.getName());
        }
        map.killMonster(potatoMonster, null, false);
        potatoMonster = null;
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.addGenericEvent(this);
        if (potatoMonster == null) {
            potatoMonster = MapleLifeFactory.getMonster(PotatoMonsterID);
            potatoMonster.setPosition(player.getPosition());
            map.spawnFakeMonster(potatoMonster);
        }
        potatoHolder = player.getId();
        player.sendMessage("You are now holding the hot potato");
        MobSkill mskill = MobSkillFactory.getMobSkill(123, 1);
        mskill.setDuration(4000);
        player.giveDebuff(MapleDisease.STUN, mskill);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.sendMessage("You are no longer holding the hot potato");
    }

    @PacketWorker
    public void onPlayerMoved(PlayerMoveEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        var movs = event.getMovements().stream().map(Duplicable::duplicate).collect(Collectors.toList());
        movs.forEach(m -> m.getPosition().translate(0, -65));
        map.broadcastMessage(MaplePacketCreator.moveMonster(0, -1, 0, 0, 0, 0,
                potatoMonster.getObjectId(), event.getClientPosition(), movs));
        if (System.currentTimeMillis() - lastSwap < 4000) {
            return;
        }
        event.onPost(new Runnable() {
            @Override
            public void run() {
                Collection<MapleCharacter> characters = map.getPlayers(p -> p.getId() != player.getId());
                if (!characters.isEmpty()) {
                    for (MapleCharacter players : characters) {
                        final double distance = players.getPosition().distance(player.getPosition());
                        if (players != player && distance < 40) {
                            if (!players.isGM() || players.isDebug()) {
                                lastSwap = System.currentTimeMillis();
                                unregisterPlayer(player);
                                registerPlayer(players);
                                map.sendPacket(MaplePacketCreator.playSound("Romio/discovery"));
                                map.sendMessage(5, "{} has tagged {}", player.getName(), players.getName());
                                break;
                            }
                        }
                    }
                }
                characters.clear();
            }
        });
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }
}
