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
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.world.MaplePartyCharacter;
import scripting.item.ItemScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.scriptedItem;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.quest.custom.CQuestData;
import server.quest.custom.requirement.CQuestItemRequirement;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public final class ItemPickupHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.readInt(); //Timestamp
        slea.readByte();
        slea.readPos(); //cpos
        int oid = slea.readInt();
        MapleCharacter chr = c.getPlayer();
        MapleMapObject ob = chr.getMap().getMapObject(oid);
        if (ob == null) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            if (System.currentTimeMillis() - mapitem.getDropTime() < 900) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getItemId() == 4031865 || mapitem.getItemId() == 4031866 || mapitem.getMeso() > 0 || ii.isConsumeOnPickup(mapitem.getItemId()) || MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if ((chr.getMapId() > 209000000 && chr.getMapId() < 209000016) || (chr.getMapId() >= 990000500 && chr.getMapId() <= 990000502)) {//happyville trees and guild PQ
                    if (!mapitem.isPlayerDrop() || mapitem.getDropper().getObjectId() == c.getPlayer().getObjectId()) {
                        if (mapitem.getMeso() > 0) {
                            chr.gainMeso(mapitem.getMeso(), true, true, false);
                            chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
                            chr.getMap().removeMapObject(ob);
                            mapitem.setPickedUp(true);
                        } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), false)) {
                            chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
                            chr.getMap().removeMapObject(ob);
                            mapitem.setPickedUp(true);
                            if (chr.getArcade() != null) {
                                if (!chr.getArcade().fail()) {
                                    chr.getArcade().add();
                                }
                            }
                        } else {
                            c.announce(MaplePacketCreator.enableActions());
                            return;
                        }
                    } else {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                synchronized (mapitem) {
                    if (mapitem.getQuest() > 0 && !chr.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
                        c.announce(MaplePacketCreator.showItemUnavailable());
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (mapitem.isPickedUp()) {
                        c.announce(MaplePacketCreator.getInventoryFull());
                        c.announce(MaplePacketCreator.getShowInventoryFull());
                        return;
                    }
                    if (mapitem.getMeso() > 0) {
                        if (chr.getParty() != null) {
                            int mesosamm = mapitem.getMeso();
                            if (mesosamm > 50000 * chr.getMesoRate()) {
                                return;
                            }
                            int partynum = 0;
                            for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId() && partymem.getChannel() == c.getChannel()) {
                                    partynum++;
                                }
                            }
                            for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
                                if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId()) {
                                    MapleCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
                                    if (somecharacter != null) {
                                        somecharacter.gainMeso(mesosamm / partynum, true, true, false);
                                    }
                                }
                            }
                        } else {
                            chr.gainMeso(mapitem.getMeso(), true, true, false);
                        }
                    } else if (mapitem.getItem().getItemId() / 10000 == 243) {
                        scriptedItem info = ii.getScriptedItemInfo(mapitem.getItem().getItemId());
                        if (info.runOnPickup()) {
                            ItemScriptManager ism = ItemScriptManager.getInstance();
                            String scriptName = info.getScript();
                            if (ism.scriptExists(scriptName)) {
                                ism.getItemScript(c, scriptName);
                            }

                        } else {
                            if (!MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                                c.announce(MaplePacketCreator.enableActions());
                                return;
                            } else {
                                if (chr.getArcade() != null) {
                                    if (!chr.getArcade().fail()) {
                                        chr.getArcade().add();
                                    }
                                }
                            }
                        }
                    } else if (mapitem.getItemId() == 4031865 || mapitem.getItemId() == 4031866) {
                        // Add NX to account, show effect and make item disapear
                        chr.getCashShop().gainCash(1, mapitem.getItemId() == 4031865 ? 100 : 250);
                    } else if (useItem(c, mapitem.getItem().getItemId())) {
                        if (mapitem.getItem().getItemId() / 10000 == 238) {
                            chr.getMonsterBook().addCard(c, mapitem.getItem().getItemId());
                        }
                    } else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
                        if (chr.getArcade() != null) {
                            if (!chr.getArcade().fail()) {
                                chr.getArcade().add();
                            }
                        }
                        for (CQuestData data : chr.getCustomQuests().values()) {
                            if (!data.isCompleted()) {
                                CQuestItemRequirement toLoot = data.getToCollect();
                                if (toLoot.incrementRequirement(mapitem.getItemId(), mapitem.getItem().getQuantity())) { // requirement is finished
                                    boolean checked = toLoot.isFinished(); // local bool before updating requirement checks; if false, quest is not finished
                                    if (data.checkRequirements() && !checked) { // update requirement checks - it is important that checkRequirements is executed first
                                        /*
                                        If checkRequirements returns true, the quest is finished. If checked is also false, then
                                        this is check means the quest is finished. The quest completion notification should only
                                        happen once unless a progress variable drops below the requirement
                                         */
                                        c.announce(MaplePacketCreator.getShowQuestCompletion(1));
                                        c.announce(MaplePacketCreator.earnTitleMessage(String.format("Quest '%s' completed!", data.getName())));
                                    }
                                }
                                Pair<Integer, Integer> p = toLoot.get(mapitem.getItemId());
                                if (p != null) {
                                    String name = ii.getName(mapitem.getItemId());
                                    name = (name == null) ? "NO-NAME" : name; // hmmm
                                    chr.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Item Collection '%s' [%d / %d]", data.getName(), name, p.left, p.right)));
                                }
                            }
                        }
                    } else if (mapitem.getItem().getItemId() == 4031868) {
                        chr.getMap().broadcastMessage(MaplePacketCreator.updateAriantPQRanking(chr.getName(), chr.getItemQuantity(4031868, false), false));
                    } else {
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    }
                    mapitem.setPickedUp(true);
                    chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
                    chr.getMap().removeMapObject(ob);
                }
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    static boolean useItem(final MapleClient c, final int id) {
        if (id / 1000000 == 2) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (ii.isConsumeOnPickup(id)) {
                if (id > 2022430 && id < 2022434) {
                    for (MapleCharacter mc : c.getPlayer().getMap().getCharacters()) {
                        if (mc.getParty() == c.getPlayer().getParty()) {
                            ii.getItemEffect(id).applyTo(mc);
                        }
                    }
                } else {
                    ii.getItemEffect(id).applyTo(c.getPlayer());
                }
                return true;
            }
        }
        return false;
    }
}
