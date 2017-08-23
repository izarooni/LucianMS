package server.events.custom;

import client.MapleCharacter;
import client.inventory.Item;
import net.server.channel.handlers.ItemMoveHandler;
import net.server.channel.handlers.ItemPickupHandler;
import scheduler.TaskExecutor;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMap;
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
    private static final int min_x = -867;
    private static final int max_x = 866;
    private static final int pos_y = 27;

    // 7 required items to drop
    private static final int base_item = 4011009;

    // Shenron NPC ID
    private static final int npcId = 9270070;

    // Invisible monster for summon effect
    private static final int monsterId = 9500364;

    private boolean summoning = false;
    private Map<Integer, Point> balls = new HashMap<>();

    private MapleNPC npc = null;

    public DBZSummoner() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (player.getGenericEvents().stream().anyMatch(g -> (g instanceof DBZSummoner))) {
            System.out.println(String.format("Player %s is already registered in a %s generic event", player.getName(), getClass().getSimpleName()));
            return;
        }
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
    public void onItemMove(ItemMoveHandler event) {
        if (summoning) {
            return;
        }
        MapleCharacter player = event.getClient().getPlayer();

        final MapleMap map = player.getMap();
        final Point position = player.getPosition().getLocation();
        if (event.getAction() == 0) { // item drop
            Item item = player.getInventory(event.getInventoryType()).getItem(event.getSource());
            if (item.getItemId() == (base_item + balls.size())) { // dropped item is a ball
                if (position.x >= min_x && position.x <= max_x && position.y == pos_y) { // ball is dropped within set boundaries
                    if (balls.size() == 6) { // all balls have been dropped
                        event.getClient().getWorldServer().broadcastPacket(MaplePacketCreator.serverNotice(6, player.getName() + " summoned the eternal Shenron! Let's hope they choose wisely"));
                        System.out.println(String.format("%s summoned Shenron on %s", player.getName(), Calendar.getInstance().getTime().toString()));
                        summoning = true;

                        // create & position the npc & monster
                        MapleMonster monster = createMonster();
                        npc = createNpc();
                        map.addMapObject(npc);

                        // use an invisible monster for a summon effect and immediately remove it after animation
                        player.announce(MaplePacketCreator.spawnMonster(monster, false, 30));

                        // currently dropping an item; we have to wait a second for the item to be added to the map's array of map-objects
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
                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println(String.format("%s timed-out Shenron on %s", player.getName(), Calendar.getInstance().getTime().toString()));
                                map.removeMapObject(npc); // remove from map to disable speaking via packet editing
                                player.announce(MaplePacketCreator.removeNPC(npc.getObjectId())); // remove NPC
                            }
                        }, 1000 * 60 * 3);
                    } else if (item.getItemId() > base_item) { // not the first ball
                        Point previous = balls.get(item.getItemId() - 1); // get the position of the previous ball
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

    private MapleNPC createNpc() {
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        npc.setPosition(new Point(0, 50));
        npc.setCy(npc.getPosition().y);
        npc.setRx0(npc.getPosition().x - 50);
        npc.setRx1(npc.getPosition().x + 50);
        return npc;
    }

    private MapleMonster createMonster() {
        MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
        if (monster != null) {
            monster.setObjectId(Integer.MAX_VALUE);
            monster.setPosition(new Point(0, 36));
            monster.setRx0(createNpc().getPosition().x);
            monster.setRx1(createNpc().getPosition().y);
        }
        return monster;
    }
}
