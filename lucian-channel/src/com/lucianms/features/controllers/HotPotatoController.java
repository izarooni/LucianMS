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
import java.util.Optional;
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

    /**
     * beings the timer for automatically ending the Hot Potato mini-game
     */
    public void start() {
        map.sendPacket(MaplePacketCreator.getClock(180));
        timeoutTask = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                MapleCharacter holder = map.getCharacterById(potatoHolder);
                if (holder != null) {
                    holder.setHpMp(0);
                    unregisterPlayer(holder);
                    map.sendMessage(5, "{} has lost the game of Hot Potato!", holder.getName());
                }
                end();
            }
        }, TimeUnit.MINUTES.toMillis(3));
    }

    /**
     * kills and unregisters the holder then kills the potato
     */
    private void end() {
        timeoutTask = TaskExecutor.cancelTask(timeoutTask);
        map.killMonster(potatoMonster, null, false);
        potatoMonster = null;
    }

    @Override
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        onPlayerDisconnect(player);
        return false;
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        onPlayerDisconnect(player);
        return true;
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        unregisterPlayer(player);
        Collection<MapleCharacter> players = map.getPlayers(p -> p.getId() != player.getId());
        Optional<MapleCharacter> any = players.stream().findAny();
        any.ifPresentOrElse(this::unregisterPlayer, this::dispose);
        players.clear();
    }

    /**
     * Changes the hot potato holder
     *
     * @param player the player to hold the potato
     */
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

    /**
     * Removes the player from the generic event so the packet events are no longer being delegaeted
     *
     * @param player the player to remove from the hot potato mini-game
     */
    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.sendMessage("You are no longer holding the hot potato");
    }

    @PacketWorker
    public void onPlayerMoved(PlayerMoveEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        // translate the monster to above the player
        var movs = event.getMovements().stream().map(Duplicable::duplicate).collect(Collectors.toList());
        movs.forEach(m -> m.getPosition().translate(0, -65));
        // re-send the movement packets from the player as the potato monster
        map.broadcastMessage(MaplePacketCreator.moveMonster(0, -1, 0, 0, 0, 0,
                potatoMonster.getObjectId(), event.getClientPosition(), movs));
        if (System.currentTimeMillis() - lastSwap < 4000) {
            // grace period from being tagged again
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
