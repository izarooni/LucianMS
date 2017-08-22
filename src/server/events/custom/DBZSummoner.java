package server.events.custom;

import client.MapleCharacter;
import client.inventory.Item;
import net.server.channel.handlers.ItemMoveHandler;
import net.server.channel.handlers.ItemPickupHandler;
import scheduler.TaskExecutor;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

import java.awt.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class DBZSummoner extends GenericEvent {

    // platform range
    private static final int min_x = -324;
    private static final int max_x = 196;
    private static final int pos_y = 27;

    private static final int base_item = 4011009;

    private static final int npcId = 9270070;
    private static final Point npcPosition = new Point(0, 50);

    private static final int monsterId = 9500364;
    private static final Point monsterPosition = new Point(0, 36);

    private boolean summoning = false;
    private Map<Integer, Point> balls = new HashMap<>();

    public DBZSummoner() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    @PacketWorker
    public void onItemMove(ItemMoveHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        final Point position = player.getPosition().getLocation();
        if (event.getAction() == 0) { // item drop
            Item item = player.getInventory(event.getInventoryType()).getItem(event.getSource());
            if (item.getItemId() == (base_item + balls.size())) { // dropped item is a ball
                if (position.x >= min_x && position.x <= max_x && position.y == pos_y) {
                    if (balls.size() == 6) { // all balls have been dropped

                        player.announce(MaplePacketCreator.getClock(180));

                        // create and position NPC
                        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                        npc.setPosition(npcPosition.getLocation());
                        npc.setCy(npcPosition.y);
                        npc.setRx0(npcPosition.x - 50);
                        npc.setRx1(npcPosition.x + 50);
                        player.getMap().addMapObject(npc);

                        // create and position effect (monster)
                        MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
                        if (monster != null) {
                            monster.setObjectId(Integer.MAX_VALUE);
                            monster.setPosition(monsterPosition.getLocation());
                            monster.setRx0(monsterPosition.x);
                            monster.setRx1(monsterPosition.y);
                            // use an invisible monster for a summon effect and immediately remove it after animation
                            player.announce(MaplePacketCreator.spawnMonster(monster, false, 30));
                            System.out.println("Shenron was summoned by " + player.getName() + " at " + Calendar.getInstance().getTime());
                            summoning = true;
                        }

                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                player.getMap().clearDrops();
                                player.announce(MaplePacketCreator.killMonster(Integer.MAX_VALUE, false));
                                npc.sendSpawnData(event.getClient());
                                unregisterPlayer(player);
                            }
                        }, 1000 * 60 * 3);

                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                player.announce(MaplePacketCreator.removeNPC(npc.getObjectId()));
                                player.getMap().removeMapObject(npc);
                            }
                        }, 1000 * 60 * 3);
                    } else if (item.getItemId() > base_item) { // not the first ball
                        Point previous = balls.get(item.getItemId() - 1);
                        if (position.x - previous.x > 50) { // drop from left to right
                            return; // dropped ball is too far from the previous ball location
                        }
                    }
                    balls.put(item.getItemId(), position);
                }
            }
        }
    }

    @PacketWorker
    public void onItemLoot(ItemPickupHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        MapleMapObject object = player.getMap().getMapObject(event.getObjectId());
        if (object instanceof MapleMapItem) {
            MapleMapItem mapItem = (MapleMapItem) object;
            if (mapItem.getItemId() >= base_item && mapItem.getItemId() <= base_item + 6) {
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
}
