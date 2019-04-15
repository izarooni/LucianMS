/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Item;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
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
 * @author Matze
 */
public final class
ItemPickupEvent extends PacketEvent {

    private int objectId;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        reader.skip(1);
        reader.skip(4);
        objectId = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        MapleMapObject ob = map.getMapObject(objectId);
        if (ob == null) {
            return null;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            if (System.currentTimeMillis() - mapitem.getDropTime() < 900) {
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            }
            // un-obtainable item
            if (mapitem.getItem() != null && !mapitem.getItem().isObtainable()) {
                Item item = mapitem.getItem();
                if (item.getItemId() == 3990022) {
                    player.dropMessage("Foothold ID: " + item.getOwner().split(":")[1]);
                }
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                map.removeMapObject(ob);
                getClient().announce(MaplePacketCreator.enableActions());
                return null;
            }
            if (mapitem.getItemId() == 4031865 || mapitem.getItemId() == 4031866 || mapitem.getMeso() > 0 || ii.isConsumeOnPickup(mapitem.getItemId()) || MapleInventoryManipulator.checkSpace(getClient(), mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if ((player.getMapId() > 209000000 && player.getMapId() < 209000016) || (player.getMapId() >= 990000500 && player.getMapId() <= 990000502)) {//happyville trees and guild PQ
                    if (!mapitem.isPlayerDrop() || mapitem.getDropper().getObjectId() == player.getObjectId()) {
                        if (mapitem.getMeso() > 0) {
                            player.gainMeso(mapitem.getMeso(), true, true, false);
                            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                            map.removeMapObject(ob);
                            mapitem.setPickedUp(true);
                        } else if (MapleInventoryManipulator.addFromDrop(getClient(), mapitem.getItem(), false)) {
                            map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                            map.removeMapObject(ob);
                            mapitem.setPickedUp(true);
                            if (player.getArcade() != null) {
                                if (player.getArcade().getId() == 4) {
                                    if (!player.getArcade().fail()) {
                                        player.getArcade().add();
                                    }
                                }
                            }
                        } else {
                            getClient().announce(MaplePacketCreator.enableActions());
                            return null;
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.getInventoryFull());
                        getClient().announce(MaplePacketCreator.getShowInventoryFull());
                        return null;
                    }
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }

                if (mapitem.getQuest() > 0 && !player.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
                    getClient().announce(MaplePacketCreator.showItemUnavailable());
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                if (mapitem.isPickedUp()) {
                    getClient().announce(MaplePacketCreator.getInventoryFull());
                    getClient().announce(MaplePacketCreator.getShowInventoryFull());
                    return null;
                }
                if (mapitem.getMeso() > 0) {
                    MapleParty party = player.getParty();
                    if (party != null) {
                        int mesosamm = mapitem.getMeso();
                        if (mesosamm > 50000 * player.getMesoRate()) {
                            return null;
                        }
                        Collection<MapleCharacter> players = party.getPlayers(p -> p.getMap() == player.getMap());
                        for (MapleCharacter partymem : players) {
                            partymem.gainMeso(mesosamm / players.size(), true, true, false);
                        }
                        for (MapleCharacter partymem : players) {
                            partymem.gainMeso(mesosamm / players.size(), true, true, false);
                        }
                    } else {
                        player.gainMeso(mapitem.getMeso(), true, true, false);
                    }
                } else if (mapitem.getItemId() / 10000 == 243) {
                    scriptedItem info = ii.getScriptedItemInfo(mapitem.getItemId());
                    if (info.runOnPickup()) {
                        ItemScriptManager ism = ItemScriptManager.getInstance();
                        String scriptName = info.getScript();
                        if (ism.scriptExists(scriptName)) {
                            ism.getItemScript(getClient(), scriptName);
                        }

                    } else {
                        if (!MapleInventoryManipulator.addFromDrop(getClient(), mapitem.getItem(), true)) {
                            getClient().announce(MaplePacketCreator.enableActions());
                            return null;
                        } else {
                            if (player.getArcade() != null) {
                                if (!player.getArcade().fail()) {
                                    player.getArcade().add();
                                }
                            }
                        }
                    }
                } else if (mapitem.getItemId() == 4031865 || mapitem.getItemId() == 4031866) {
                    // Add NX to account, show effect and make item disapear
                    player.getCashShop().gainCash(1, mapitem.getItemId() == 4031865 ? 100 : 250);
                } else if (useItem(getClient(), mapitem.getItemId())) {
                    if (mapitem.getItemId() / 10000 == 238) {
                        player.getMonsterBook().addCard(getClient(), mapitem.getItemId());
                    }
                } else if (MapleInventoryManipulator.addFromDrop(getClient(), mapitem.getItem(), true)) {
                    if (player.getArcade() != null) {
                        if (!player.getArcade().fail()) {
                            player.getArcade().add();
                        }
                    }
                    for (CQuestData data : player.getCustomQuests().values()) {
                        if (!data.isCompleted()) {
                            CQuestItemRequirement toLoot = data.getToCollect();
                            toLoot.incrementRequirement(mapitem.getItemId(), mapitem.getItem().getQuantity());
                            boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                            if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                                    /*
                                    If checkRequirements returns true, the quest is finished. If checked is also false, then
                                    this is check means the quest is finished. The quest completion notification should only
                                    happen once unless a progress variable drops below the requirement
                                     */
                                data.announceCompletion(getClient());
                            }
                            if (!data.isSilentComplete()) {
                                CQuestItemRequirement.CQuestItem p = toLoot.get(mapitem.getItemId());
                                if (p != null) {
                                    String name = ii.getName(mapitem.getItemId());
                                    name = (name == null) ? "NO-NAME" : name; // hmmm
                                    player.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, p.getProgress(), p.getRequirement())));
                                }
                            }
                        }
                    }
                } else if (mapitem.getItemId() == 4031868) {
                    map.broadcastMessage(MaplePacketCreator.updateAriantPQRanking(player.getName(), player.getItemQuantity(4031868, false), false));
                } else {
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                mapitem.setPickedUp(true);
                map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, player.getId()), mapitem.getPosition());
                map.removeMapObject(ob);
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }

    public int getObjectId() {
        return objectId;
    }

    public static boolean useItem(final MapleClient c, final int id) {
        if (id / 1000000 == 2) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (ii.isConsumeOnPickup(id)) {
                MapleCharacter player = c.getPlayer();
                if (id > 2022430 && id < 2022434) {
                    Collection<MapleCharacter> characters = player.getMap().getCharacters();
                    try {
                        for (MapleCharacter mc : characters) {
                            if (mc.getParty() == player.getParty()) {
                                ii.getItemEffect(id).applyTo(mc);
                            }
                        }
                    } finally {
                        characters.clear();
                    }
                } else {
                    ii.getItemEffect(id).applyTo(player);
                }
                return true;
            }
        }
        return false;
    }
}
