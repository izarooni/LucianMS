package com.lucianms.features.summoning;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.events.ItemPickupEvent;
import com.lucianms.events.PlayerInventoryMoveEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.maps.MapleMapObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class ShenronSummoner extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShenronSummoner.class);

    //region constants
    private static final int SummoningMap = 908;
    // platform range
    private static final int min_x = -867;
    private static final int max_x = 866;
    private static final int pos_y = 27;
    // 7 required items to drop
    private static final int ReactionItemBase = 4011009;
    // Shenron NPC ID
    private static final int SummoningNpc = 9270070;
    // Invisible monster for summon effect
    private static final int SummoningMonster = 9500364;
    //endregion

    // Items that have dropped so far
    private Map<Integer, Point> balls = new HashMap<>();
    private Task destroyTask;
    // Object to summon
    private MapleNPC npc;
    private boolean summoning;
    private boolean wishing;

    public ShenronSummoner() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.getGenericEvents().stream().filter(g -> g instanceof ShenronSummoner).findFirst().ifPresent(player::removeGenericEvent);
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        if (npc != null) {
            player.getMap().removeMapObject(npc);
            player.announce(MaplePacketCreator.removeNPC(npc.getObjectId()));
        }
        player.announce(MaplePacketCreator.removeClock());
    }

    @PacketWorker
    public void onItemMove(PlayerInventoryMoveEvent event) {
        if (summoning) {
            return;
        }
        MapleCharacter player = event.getClient().getPlayer();

        final MapleMap map = player.getMap();
        final Point position = player.getPosition().getLocation();
        if (event.getAction() == 0) { // item drop
            Item item = player.getInventory(event.getInventoryType()).getItem(event.getSource());
            if (item.getItemId() == (ReactionItemBase + balls.size())) { // dropped item is a ball
                if (position.x >= min_x && position.x <= max_x && position.y == pos_y) { // ball is dropped within set boundaries
                    if (balls.size() == 6) { // all balls have been dropped
                        event.getClient().getWorldServer().sendMessage(6, "{} summoned the eternal Shenron! Let's hope they choose wisely", player.getName());
                        player.announce(MaplePacketCreator.playSound("Shenron/summoning"));
                        summoning = true;
                        wishing = true;

                        // create & position the npc & monster
                        MapleMonster monster = createMonster();
                        npc = createNpc();
                        map.addMapObject(npc);

                        // use an invisible monster for a summon effect and immediately remove it after animation
                        player.announce(MaplePacketCreator.spawnMonster(monster, false, 30));

                        // currently dropping an item; we have to wait a second for the item to be added to the map's array of map-objects
                        // todo store object-IDs and clear specifics instead of all map drops
                        TaskExecutor.createTask(map::clearDrops, 1500);

                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                if (monster != null) {
                                    player.announce(MaplePacketCreator.killMonster(Integer.MAX_VALUE, false)); // summon effect finished; no longer need monster
                                }
                                player.announce(MaplePacketCreator.getClock(180));
                                player.announce(MaplePacketCreator.spawnNPC(npc)); // don't use NPC controller, otherwise NPC will not be able to de-spawn
                            }
                        }, 3100); // animation length

                        // de-spawn timer
                        destroyTask = TaskExecutor.cancelTask(destroyTask);
                        destroyTask = TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                wishing = false;
                                if (player.getMapId() == SummoningMap) {
                                    map.removeMapObject(npc);
                                }
                                player.announce(MaplePacketCreator.removeNPC(npc.getObjectId())); // remove NPC
                            }
                        }, 1000 * 60 * 3);
                    } else if (item.getItemId() > ReactionItemBase) { // not the first ball
                        Point previous = balls.get(item.getItemId() - 1); // get the position of the previous ball
                        if (Math.abs(position.x - previous.x) > 75) { // drop from left to right
                            return; // dropped ball is too far from the previous ball location
                        }
                    }
                    balls.put(item.getItemId(), position);
                }
            }
        }
    }

    @PacketWorker
    public void onItemLoot(ItemPickupEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        MapleMapObject object = player.getMap().getMapObject(event.getObjectId());
        if (object instanceof MapleMapItem) {
            MapleMapItem mapItem = (MapleMapItem) object;
            if (mapItem.getItemId() >= ReactionItemBase && mapItem.getItemId() <= ReactionItemBase + 6) {
                if (summoning) {
                    // kinda like smuggling, don't allow looting of dragon balls while summoning Shenron
                    event.setCanceled(true);
                } else {
                    if (!balls.isEmpty()) {
                        player.dropMessage("The order has been broken! Please collect all balls and drop them in the correct order");
                        balls.clear();
                        balls = new HashMap<>();
                    }
                }
            }
        }
    }

    private MapleNPC createNpc() {
        MapleNPC npc = MapleLifeFactory.getNPC(SummoningNpc);
        npc.setPosition(new Point(0, 50));
        npc.setCy(npc.getPosition().y);
        npc.setRx0(npc.getPosition().x - 50);
        npc.setRx1(npc.getPosition().x + 50);
        npc.setHide(true);
        return npc;
    }

    private MapleMonster createMonster() {
        MapleMonster monster = MapleLifeFactory.getMonster(SummoningMonster);
        if (monster != null) {
            monster.setObjectId(Integer.MAX_VALUE);
            monster.setPosition(new Point(0, 36));
            monster.setRx0(createNpc().getPosition().x);
            monster.setRx1(createNpc().getPosition().y);
        }
        return monster;
    }

    public boolean isWishing() {
        return wishing;
    }

    public void setWishing(boolean wishing) {
        this.wishing = wishing;
    }

    public void wish(MapleCharacter player) {
        wishing = false;
        summoning = false;
        balls.clear();

        npc.sendDestroyData(player.getClient());
        player.getMap().removeMapObject(npc.getObjectId());
        player.announce(MaplePacketCreator.playSound("Shenron/wishgranted"));

        destroyTask = TaskExecutor.cancelTask(destroyTask);
    }
}
