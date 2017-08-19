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
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class DBZSummoner extends GenericEvent {

    // platform range
    private static final int min_x = -57;
    private static final int max_x = 118;
    private static final int pos_y = 114;
    private static final int base_item = 4011009;
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
                        MapleNPC npc = MapleLifeFactory.getNPC(2001);
                        npc.setPosition(new Point(-52, 0));
                        player.getMap().addMapObject(npc);

                        MapleMonster monster = MapleLifeFactory.getMonster(9500364);
                        if (monster != null) {
                            monster.setObjectId(Integer.MAX_VALUE);
                            monster.setPosition(new Point(0, 50));
                            player.announce(MaplePacketCreator.spawnMonster(monster, false, 30));
                        }

                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                npc.sendSpawnData(event.getClient());
                                player.announce(MaplePacketCreator.killMonster(Integer.MAX_VALUE, false));
                                player.getMap().clearDrops();
                                unregisterPlayer(player);
                            }
                        }, 1720);
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
                if (!balls.isEmpty()) {
                    player.dropMessage("The order has been broken! Please collect all balls and drop them in the correct order");
                    balls.clear();
                    balls = new HashMap<>();
                }
            }
        }
    }

    public Map<Integer, Point> getBalls() {
        return balls;
    }
}
