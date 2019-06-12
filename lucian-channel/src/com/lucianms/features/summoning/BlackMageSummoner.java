package com.lucianms.features.summoning;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.ItemPickupEvent;
import com.lucianms.events.PlayerInventoryMoveEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.maps.MapleMapObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class BlackMageSummoner extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackMageSummoner.class);

    //region constants
    private static final int SummoningMap = 97;
    private static final int ReactionItem = 4011022;
    private static final int SummoningMonster = 9895253;
    //endregion

    private int reactionItemOID = 0;
    private boolean summoning = false;

    public BlackMageSummoner() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.getGenericEvents().stream().filter(g -> g instanceof BlackMageSummoner).findFirst().ifPresent(player::removeGenericEvent);

        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        unregisterPlayer(player);
        return true;
    }

    @PacketWorker
    public void onItemMove(PlayerInventoryMoveEvent event) {
        event.setCanceled(true);
        if (summoning) {
            return;
        }
        Object result = event.onPacket();
        if (result != null && result instanceof MapleMapItem) {
            MapleMapItem mapItem = (MapleMapItem) result;
            MapleCharacter player = event.getClient().getPlayer();

            reactionItemOID = mapItem.getObjectId();

            final MapleMap map = player.getMap();
            if (event.getAction() == 0) { // dropping item
                if (mapItem.getItemId() == ReactionItem) { // dropped item is a ball
                    summoning = true;
                    TaskExecutor.createTask(new Runnable() {
                        @Override
                        public void run() {
                            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 0, 0), mapItem.getPosition());
                            map.removeMapObject(mapItem);

                            MapleMonster monster = MapleLifeFactory.getMonster(SummoningMonster);
                            if (monster != null) {
                                map.spawnMonsterOnGroudBelow(monster, mapItem.getPosition());
                            } else {
                                LOGGER.warn("Attempt to summon black mage - monster({}) does not exist!", SummoningMonster);
                            }
                        }
                    }, 2000);
                }
            }
        }
    }

    @PacketWorker
    public void onItemPickup(ItemPickupEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        MapleMapObject object = player.getMap().getMapObject(event.getObjectId());
        if (object instanceof MapleMapItem) {
            if (object.getObjectId() == reactionItemOID) {
                player.dropMessage("This item is currently unavailable for pick-up");
                event.setCanceled(true);
            }
        }
    }
}
