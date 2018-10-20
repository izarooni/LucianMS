package com.lucianms.features.auto;

import client.MapleCharacter;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.events.channel.PlayerAllChatEvent;
import net.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class AInfinityPurge extends GAutoEvent {

    private static final int EventFieldID = 109020001;

    private boolean joinable = true;
    private boolean commandsEnabled = false;
    private Task tEvent = null;
    private Hashtable<String, Integer> scores = new Hashtable<>(25);

    public AInfinityPurge(MapleWorld world) {
        super(world, true);
    }

    public void delayEnableCommands() {
        tEvent = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                commandsEnabled = true;
            }
        }, 1000 * 60 * 3);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Infinity Purge is starting in 1 minute!");
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                joinable = false;
            }
        }, 1000 * 60);
    }

    @Override
    public void stop() {
        TaskExecutor.cancelTask(tEvent);
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        if (!joinable) {
            player.sendMessage(5, "Sorry but this even is beginning soon thus no longer open!");
            return;
        }
        scores.put(player.getName(), 0);
        player.changeMap(getMapInstance(EventFieldID));
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        scores.remove(player.getName());
    }

    @PacketWorker
    public void onALlChat(PlayerAllChatEvent event) {
        if (!commandsEnabled) {
            return;
        }
        MapleCharacter player = event.getClient().getPlayer();
        String message = event.getContent();
        Integer score = scores.computeIfAbsent(player.getName(), n -> 0);

        if (message.equalsIgnoreCase("tag!")) {
            for (MapleCharacter players : player.getMap().getMapObjects(MapleCharacter.class)) {
                if (players.isAlive() &&  players.getPosition().distanceSq(player.getPosition()) <= 20000) {
                    scores.put(player.getName(), score + 1);
                    player.setHpMp(0);
                    player.getMap().broadcastMessage(
                            MaplePacketCreator.serverNotice(5,
                                    String.format("'%s' tagged '%s' and now has a score of %d", player.getName(), players.getName(), score + 1))
                    );
                    break;
                }
            }
            event.setCanceled(true);
        }
    }
}
