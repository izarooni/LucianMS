package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.io.scripting.item.ItemScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleItemInformationProvider.scriptedItem;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.world.MapleParty;
import tools.MaplePacketCreator;

import java.util.Collection;

/**
 * @author TheRamon
 * @author izarooni
 */
public class PlayerPetPickupEvent extends PacketEvent {

    private int petID;
    private int objectID;

    @Override
    public void processInput(MaplePacketReader reader) {
        petID = reader.readInt();
        reader.skip(13);
        objectID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MaplePet pet = player.getPet(player.getPetIndex(petID));
        if (pet == null || !pet.isSummoned()) {
            return null;
        }

        MapleMap map = player.getMap();
        MapleMapObject ob = map.getMapObject(objectID);
        if (ob == null) {
            getClient().announce(MaplePacketCreator.getInventoryFull());
            return null;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            if (!player.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
                getClient().announce(MaplePacketCreator.showItemUnavailable());
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            }
            if (System.currentTimeMillis() - mapitem.getDropTime() < 900) {
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            }
            if (mapitem.isPickedUp()) {
                getClient().announce(MaplePacketCreator.getInventoryFull());
                return null;
            }
            if (mapitem.getDropper() == player) {
                return null;
            }
            if (mapitem.getMeso() > 0) {
                MapleParty party = player.getParty();
                if (party != null) {
                    int mesosamm = mapitem.getMeso();
                    if (mesosamm > 50000 * player.getMesoRate()) return null;
                    Collection<MapleCharacter> players = party.getPlayers(p -> p.getMap() == player.getMap());
                    for (MapleCharacter partymem : players) {
                        partymem.gainMeso(mesosamm / players.size(), true, true, false);
                    }
                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                    map.removeMapObject(ob);
                } else if (player.getInventory(MapleInventoryType.EQUIPPED).findById(1812000) != null) {
                    player.gainMeso(mapitem.getMeso(), true, true, false);
                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                    map.removeMapObject(ob);
                } else {
                    mapitem.setPickedUp(false);
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
            } else if (ItemPickupEvent.useItem(getClient(), mapitem.getItem().getItemId())) {
                if (mapitem.getItem().getItemId() / 10000 == 238) {
                    player.getMonsterBook().addCard(getClient(), mapitem.getItem().getItemId());
                }
                mapitem.setPickedUp(true);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                map.removeMapObject(ob);
            } else if (mapitem.getItem().getItemId() / 100 == 50000) {
                if (player.getInventory(MapleInventoryType.EQUIPPED).findById(1812007) != null) {
                    for (int i : player.getExcluded()) {
                        if (mapitem.getItem().getItemId() == i) {
                            return null;
                        }
                    }
                } else if (MapleInventoryManipulator.addById(getClient(), mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, -1, mapitem.getItem().getExpiration())) {
                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                    map.removeMapObject(ob);
                } else {
                    return null;
                }
            } else if (mapitem.getItem().getItemId() / 10000 == 243) {
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                scriptedItem info = ii.getScriptedItemInfo(mapitem.getItem().getItemId());
                if (info.runOnPickup()) {
                    ItemScriptManager ism = ItemScriptManager.getInstance();
                    String scriptName = info.getScript();
                    if (ism.scriptExists(scriptName))
                        ism.getItemScript(getClient(), scriptName);

                } else {
                    MapleInventoryManipulator.addFromDrop(getClient(), mapitem.getItem(), true);
                }
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                map.removeMapObject(ob);
            } else if (mapitem.getItemId() == 4031865 || mapitem.getItemId() == 4031866) {
                // Add NX to account, show effect and make item disapear
                player.getCashShop().gainCash(1, mapitem.getItemId() == 4031865 ? 100 : 250);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                map.removeMapObject(ob);
            } else if (MapleInventoryManipulator.addFromDrop(getClient(), mapitem.getItem(), true)) {
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, player.getId(), true, player.getPetIndex(pet)), mapitem.getPosition());
                map.removeMapObject(ob);
            } else {
                return null;
            }
            mapitem.setPickedUp(true);
        }
        return null;
    }
}
