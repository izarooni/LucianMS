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
package com.lucianms.io.scripting.reactor;

import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.io.scripting.AbstractPlayerInteraction;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapMonitor;
import com.lucianms.server.maps.MapleReactor;
import com.lucianms.server.maps.ReactorDropEntry;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestItemRequirement;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Lerk
 */
public class ReactorActionManager extends AbstractPlayerInteraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorActionManager.class);
    private MapleReactor reactor;
    private MapleClient client;

    public ReactorActionManager(MapleClient client, MapleReactor reactor) {
        super(client);
        this.reactor = reactor;
        this.client = client;
    }

    public void dropItem(int itemId, short quantity) {
        MapleInventoryType iType = ItemConstants.getInventoryType(itemId);
        if (iType == MapleInventoryType.UNDEFINED) {
            LOGGER.error("Invalid item ID specified '{}' -- no inventory type", itemId);
            return;
        }
        Item item;
        if (iType == MapleInventoryType.EQUIP) {
            item = MapleItemInformationProvider.getInstance().getEquipById(itemId);
            if (item == null) {
                LOGGER.error("Unable to retrieve equip stats with specified item ID '{}'", itemId);
                return;
            }
        } else {
            item = new Item(itemId, (short) 0, quantity);
        }
        reactor.getMap().spawnItemDrop(reactor, client.getPlayer(), item, reactor.getPosition(), true, false);
    }

    public void dropItems() {
        dropItems(false, 0, 0, 0, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0);
    }

    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
        List<ReactorDropEntry> chances = getDropChances();
        List<ReactorDropEntry> items = new ArrayList<>();
        try {
            int numItems = 0;
            if (meso && Math.random() < (1 / (double) mesoChance)) {
                items.add(new ReactorDropEntry(0, mesoChance, -1));
            }
            for (ReactorDropEntry d : chances) {
                if (getPlayer().isDebug() || Math.random() < (1 / (double) d.chance)) {
                    numItems++;
                    items.add(d);
                }
            }
            for (CQuestData qData : client.getPlayer().getCustomQuests().values()) {
                if (!qData.isCompleted()) {
                    for (CQuestItemRequirement.CQuestItem qItem : qData.getToCollect().getItems().values()) {
                        if (qItem.getReactorId() == reactor.getId()) {
                            if (Randomizer.nextInt(100) < qItem.getChance()) {
                                int quantity = Randomizer.rand(qItem.getMinQuantity(), qItem.getMaxQuantity());
                                for (int i = 0; i < quantity; i++) {
                                    items.add(new ReactorDropEntry(qItem.getItemId(), 0, -1));
                                }
                            }
                        }
                    }
                }
            }
            while (items.size() < minItems) {
                items.add(new ReactorDropEntry(0, mesoChance, -1));
                numItems++;
            }
            java.util.Collections.shuffle(items);
            final Point dropPos = reactor.getPosition();
            dropPos.x -= (12 * numItems);
            for (ReactorDropEntry d : items) {
                if (d.itemId == 0) {
                    int range = maxMeso - minMeso;
                    int displayDrop = (int) (Math.random() * range) + minMeso;
                    int mesoDrop = (displayDrop * client.getWorldServer().getMesoRate());
                    reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, reactor, client.getPlayer(), false, (byte) 0);
                } else {
                    Item drop;
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    if (ii.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
                        drop = new Item(d.itemId, (short) 0, (short) 1);
                    } else {
                        drop = ii.randomizeStats(ii.getEquipById(d.itemId));
                    }
                    reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, false);
                }
                dropPos.x += 25;
            }
        } finally {
            items.clear();
        }
    }

    private List<ReactorDropEntry> getDropChances() {
        return ReactorScriptManager.getDrops(reactor.getId());
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    public void createMapMonitor(int mapId, String portal) {
        new MapMonitor(client.getChannelServer().getMap(mapId), portal);
    }

    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    private void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    public void spawnNpc(int npcId, Point pos) {
        MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc != null) {
            npc.setPosition(pos);
            npc.setCy(pos.y);
            npc.setRx0(pos.x + 50);
            npc.setRx1(pos.x - 50);
            npc.setFh(reactor.getMap().getFootholds().findBelow(pos).getId());
            reactor.getMap().addMapObject(npc);
            reactor.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
        }
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnFakeMonster(int id) {
        reactor.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), getPosition());
    }
}