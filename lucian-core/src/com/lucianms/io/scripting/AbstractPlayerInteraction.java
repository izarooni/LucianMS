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
package com.lucianms.io.scripting;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleQuestStatus;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.*;
import com.lucianms.constants.ItemConstants;
import com.lucianms.io.scripting.event.EventManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.Server;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.expeditions.MapleExpeditionType;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.life.MobSkillFactory;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import com.lucianms.server.partyquest.PartyQuest;
import com.lucianms.server.partyquest.Pyramid;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.MapleParty;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AbstractPlayerInteraction {

    public Connection getDatabaseConnection() throws SQLException {
        return c.getWorldServer().getConnection();
    }

    public MapleClient c;
    public ScriptObjectMirror vars;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = c;
    }

    public MapleClient getClient() {
        return c;
    }

    public MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public MapleCharacter findPlayer(String username) {
        return c.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
    }

    public void warp(int map) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
    }

    public void warp(int map, int portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public void warp(int map, String portal) {
        getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
    }

    public Task delayCall(Runnable runnable, long delay) {
        return TaskExecutor.createTask(runnable, delay);
    }

    public void warpMap(int map) {
        getPlayer().getMap().warpEveryone(map);
    }

    public void warpParty(int id) {
        Collection<MapleCharacter> members = getPartyMembers();
        for (MapleCharacter mc : members) {
            if (id == 925020100) {
                mc.setDojoParty(true);
            }
            mc.changeMap(getWarpMap(id));
        }
        members.clear();
    }

    public Collection<MapleCharacter> getPartyMembers() {
        if (getParty() != null) {
            return getParty().getPlayers();
        }
        return null;
    }

    private MapleMap getWarpMap(int map) {
        MapleMap target;
        if (getPlayer().getEventInstance() == null) {
            target = c.getChannelServer().getMap(map);
        } else {
            target = getPlayer().getEventInstance().getMapInstance(map);
        }
        if (target == null) {
            target = c.getChannelServer().getMap(1000000000); // if it's null, we go to henesys
        }
        return target;
    }

    public MapleMap getMap(int map) {
        return getWarpMap(map);
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventScriptManager().getManager(event);
    }

    public boolean hasItem(int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean hasItem(int itemid, int quantity) {
        return haveItem(itemid, quantity);
    }

    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1);
    }

    public boolean haveItem(int itemid, int quantity) {
        return getPlayer().getItemQuantity(itemid, false) >= quantity;
    }

    public boolean canHold(int itemid) {
        return getPlayer().getInventory(ItemConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public void openNpc(int npcid) {
        openNpc(npcid, null);
    }

    public void openNpc(int npcid, String script) {
        NPCScriptManager.dispose(c);
        NPCScriptManager.start(c, npcid, script);
    }

    public void updateQuest(int questid, String data) {
        MapleQuestStatus status = c.getPlayer().getQuest(MapleQuest.getInstance(questid));
        status.setStatus(MapleQuestStatus.Status.STARTED);
        status.setProgress(0, data);//override old if exists
        c.getPlayer().updateQuest(status);
    }

    public MapleQuestStatus.Status getQuestStatus(int id) {
        return c.getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus();
    }

    public boolean isQuestCompleted(int quest) {
        return getQuestStatus(quest) == MapleQuestStatus.Status.COMPLETED;
    }

    public boolean isQuestStarted(int quest) {
        return getQuestStatus(quest) == MapleQuestStatus.Status.STARTED;
    }

    public int getQuestProgress(int qid) {
        return Integer.parseInt(getPlayer().getQuest(MapleQuest.getInstance(qid)).getProgress().get(0));
    }

    public void gainItem(Item item, boolean show) {
        MapleInventoryManipulator.addFromDrop(getClient(), item, show);
    }

    public void gainItem(int id, short quantity) {
        gainItem(id, quantity, false, false);
    }

    public void gainItem(int id, short quantity, boolean show) {//this will fk randomStats equip :P
        gainItem(id, quantity, false, show);
    }

    public void gainItem(int id, boolean show) {
        gainItem(id, (short) 1, false, show);
    }

    public void gainItem(int id) {
        gainItem(id, (short) 1, false, false);
    }

    public Item gainItem(int id, short quantity, boolean randomStats, boolean showMessage) {
        return gainItem(id, quantity, randomStats, showMessage, -1);
    }

    public Item gainItem(int id, short quantity, boolean randomStats, boolean showMessage, long expires) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item item = null;
        if (quantity >= 0) {
            if (!MapleInventoryManipulator.checkSpace(c, id, quantity, "")) {
                c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + ItemConstants.getInventoryType(id).name() + " inventory.");
                return null;
            }
            if (ItemConstants.getInventoryType(id) == MapleInventoryType.EQUIP) {
                item = ii.getEquipById(id);
            } else {
                item = new Item(id, (short) 0, quantity);
            }
            if (expires != -1) {
                item.setExpiration(System.currentTimeMillis() + expires);
            }
            if (ItemConstants.isPet(id)) {
                long expiration = System.currentTimeMillis() + ((expires == -1) ? TimeUnit.DAYS.toMillis(365) : expires);
                MapleInventoryManipulator.addById(c, id, (short) 1, null, MaplePet.createPet(id), expiration);
            } else if (ItemConstants.getInventoryType(id).equals(MapleInventoryType.EQUIP) && !ItemConstants.isRechargable(item.getItemId())) {
                if (randomStats && item instanceof Equip) {
                    ii.randomizeStats((Equip) item);
                    MapleInventoryManipulator.addFromDrop(c, ii.randomizeStats((Equip) item), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                }
            } else {
                MapleInventoryManipulator.addFromDrop(c, item, false);
            }
        } else {
            MapleInventoryManipulator.removeById(c, ItemConstants.getInventoryType(id), id, -quantity, true, false);
        }
        if (showMessage) {
            c.announce(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
        return item;
    }

    public void changeMusic(String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    public void playerMessage(int type, String message) {
        c.announce(MaplePacketCreator.serverNotice(type, message));
    }

    public void message(String message) {
        getPlayer().message(message);
    }

    public void mapMessage(int type, String message) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void mapEffect(String path) {
        c.announce(MaplePacketCreator.mapEffect(path));
    }

    public void mapSound(String path) {
        c.announce(MaplePacketCreator.mapSound(path));
    }

    public void displayAranIntro() {
        String intro = "";
        switch (c.getPlayer().getMapId()) {
            case 914090010:
                intro = "Effect/Direction1.img/aranTutorial/Scene0";
                break;
            case 914090011:
                intro = "Effect/Direction1.img/aranTutorial/Scene1" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                break;
            case 914090012:
                intro = "Effect/Direction1.img/aranTutorial/Scene2" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                break;
            case 914090013:
                intro = "Effect/Direction1.img/aranTutorial/Scene3";
                break;
            case 914090100:
                intro = "Effect/Direction1.img/aranTutorial/HandedPoleArm" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                break;
            case 914090200:
                intro = "Effect/Direction1.img/aranTutorial/Maha";
                break;
        }
        showIntro(intro);
    }


    public void showIntro(String path) {
        c.announce(MaplePacketCreator.showIntro(path));
    }

    public void showInfo(String path) {
        c.announce(MaplePacketCreator.showInfo(path));
        c.announce(MaplePacketCreator.enableActions());
    }

    public void guildMessage(int type, String message) {
        if (getGuild() != null) {
            getGuild().guildMessage(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public MapleGuild getGuild() {
        return Server.getGuild(getPlayer().getGuildId(), getPlayer().getWorld(), null);
    }

    public MapleParty getParty() {
        return getPlayer().getParty();
    }

    public boolean isLeader() {
        MapleParty party = getParty();
        if (party == null) return false;
        return party.getLeaderPlayerID() == getPlayer().getId();

    }

    public void givePartyItems(int id, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            MapleClient cl = chr.getClient();
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(cl, id, quantity);
            } else {
                MapleInventoryManipulator.removeById(cl, ItemConstants.getInventoryType(id), id, -quantity, true, false);
            }
            cl.announce(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }


    public void removeHPQItems() {
        int[] items = {4001095, 4001096, 4001097, 4001098, 4001099, 4001100, 4001101};
        for (int i = 0; i < items.length; i++) {
            removePartyItems(items[i]);
        }
    }

    public void removePartyItems(int id) {
        if (getParty() == null) {
            removeAll(id);
            return;
        }
        Collection<MapleCharacter> players = getParty().getPlayers();
        players.forEach(p -> removeAll(id, p.getClient()));
        players.clear();
    }

    public void givePartyExp(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.gainExp((amount * chr.getExpRate()), true, true);
        }
    }


    public void givePartyExp(String PQ) {
        givePartyExp(PQ, true);
    }


    public void givePartyExp(String PQ, boolean instance) {
        //1 player = 0% bonus (100)
        //2 players = 0% bonus (100)
        //3 players = +0% bonus (100)
        //4 players = +10% bonus (110)
        //5 players = +20% bonus (120)
        //6 players = +30% bonus (130)
        MapleParty party = getPlayer().getParty();
        int size = party.size();

        Collection<MapleCharacter> players = party.getPlayers();
        if (instance) {
            size -= players.stream().filter(p -> p.getEventInstance() == null).mapToInt(p -> 1).sum();
        }

        int bonus = size < 4 ? 100 : 70 + (size * 10);
        for (MapleCharacter member : players) {
            if (instance && member.getEventInstance() == null) {
                continue; // They aren't in the instance, don't give EXP.
            }
            int base = PartyQuest.getExp(PQ, member.getLevel());
            int exp = base * member.getExpRate();
            exp = exp * bonus / 100;
            member.gainExp(exp, true, true);
        }
    }

    public void removeFromParty(int itemId) {
        if (getParty() == null) {
            return;
        }
        Collection<MapleCharacter> members = getPartyMembers();
        for (MapleCharacter chr : members) {
            MapleInventory iv = chr.getInventory(ItemConstants.getInventoryType(itemId));
            int count = iv.countById(itemId);
            if (count > 0) {
                MapleInventoryManipulator.removeById(c, ItemConstants.getInventoryType(itemId), itemId, count, true, false);
                chr.announce(MaplePacketCreator.getShowItemGain(itemId, (short) -count, true));
            }
        }
        members.clear();
    }

    public void removeAll(int id) {
        removeAll(id, c);
    }

    public void removeAll(int id, MapleClient cl) {
        MapleInventoryType invType = ItemConstants.getInventoryType(id);
        int possessed = cl.getPlayer().getInventory(invType).countById(id);
        if (possessed > 0) {
            MapleInventoryManipulator.removeById(cl, ItemConstants.getInventoryType(id), id, possessed, true, false);
            cl.announce(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
        }

        if (invType == MapleInventoryType.EQUIP) {
            if (cl.getPlayer().getInventory(MapleInventoryType.EQUIPPED).countById(id) > 0) {
                MapleInventoryManipulator.removeById(cl, MapleInventoryType.EQUIPPED, id, 1, true, false);
                cl.announce(MaplePacketCreator.getShowItemGain(id, (short) -1, true));
            }
        }
    }

    public int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public int getPlayerCount(int mapid) {
        return c.getChannelServer().getMap(mapid).getCharacters().size();
    }

    public void showInstruction(String msg, int width, int height) {
        c.announce(MaplePacketCreator.sendHint(msg, width, height));
        c.announce(MaplePacketCreator.enableActions());
    }

    public void disableMinimap() {
        c.announce(MaplePacketCreator.disableMinimap());
    }

    public void resetMap(int mapid) {
        getMap(mapid).resetReactors();
        getMap(mapid).killAllMonsters();
        for (MapleMapObject i : getMap(mapid).getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM))) {
            getMap(mapid).removeMapObject(i);
            getMap(mapid).broadcastMessage(MaplePacketCreator.removeItemFromMap(i.getObjectId(), 0, c.getPlayer().getId()));
        }
    }

    public void sendClock(MapleClient d, int time) {
        d.announce(MaplePacketCreator.getClock((int) (time - System.currentTimeMillis()) / 1000));
    }

    public void useItem(int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
        c.announce(MaplePacketCreator.getItemMessage(id));//Useful shet :3
    }

    public void cancelItem(final int id) {
        getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), -1, false);
    }

    public void teachSkill(int skillid, byte level, byte masterLevel, long expiration) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(skillid), level, masterLevel, expiration);
    }

    public void removeEquipFromSlot(short slot) {
        Item tempItem = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIPPED, slot, tempItem.getQuantity(), false, false);
    }

    public void gainAndEquip(int itemid, short slot) {
        final Item old = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        if (old != null) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIPPED, slot, old.getQuantity(), false, false);
        }
        final Item newItem = MapleItemInformationProvider.getInstance().getEquipById(itemid);
        newItem.setPosition(slot);
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(newItem);
        c.announce(MaplePacketCreator.modifyInventory(false, Collections.singletonList(new ModifyInventory(0, newItem))));
    }

    public void spawnMonster(int id, int x, int y) {
        getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
    }

    public MapleMonster getMonsterLifeFactory(int mid) {
        return MapleLifeFactory.getMonster(mid);
    }

    public void spawnGuide() {
        c.announce(MaplePacketCreator.spawnGuide(true));
    }

    public void removeGuide() {
        c.announce(MaplePacketCreator.spawnGuide(false));
    }

    public void displayGuide(int num) {
        c.announce(MaplePacketCreator.showInfo("UI/tutorial.img/" + num));
    }

    public void goDojoUp() {
        c.announce(MaplePacketCreator.dojoWarpUp());
    }

    public void enableActions() {
        c.announce(MaplePacketCreator.enableActions());
    }

    public void showEffect(String effect) {
        c.announce(MaplePacketCreator.showEffect(effect));
    }

    public void dojoEnergy() {
        c.announce(MaplePacketCreator.setSessionValue("energy", getPlayer().getDojoEnergy()));
    }

    public void talkGuide(String message) {
        c.announce(MaplePacketCreator.talkGuide(message));
    }

    public void guideHint(int hint) {
        c.announce(MaplePacketCreator.guideHint(hint));
    }

    public void updateAreaInfo(Short area, String info) {
        c.getPlayer().updateAreaInfo(area, info);
        c.announce(MaplePacketCreator.enableActions());//idk, nexon does the same :P
    }

    public boolean containsAreaInfo(short area, String info) {
        return c.getPlayer().containsAreaInfo(area, info);
    }

    public MobSkill getMobSkill(int skill, int level) {
        return MobSkillFactory.getMobSkill(skill, level);
    }

    public void earnTitle(String msg) {
        c.announce(MaplePacketCreator.earnTitleMessage(msg));
    }

    public void showInfoText(String msg) {
        c.announce(MaplePacketCreator.showInfoText(msg));
    }

    public void openUI(byte ui) {
        c.announce(MaplePacketCreator.openUI(ui));
    }

    public void lockUI() {
        c.announce(MaplePacketCreator.disableUI(true));
        c.announce(MaplePacketCreator.lockUI(true));
    }

    public void unlockUI() {
        c.announce(MaplePacketCreator.disableUI(false));
        c.announce(MaplePacketCreator.lockUI(false));
    }

    public void playSound(String sound) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(sound, 4));
    }

    public void environmentChange(String env, int mode) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, mode));
    }

    public Pyramid getPyramid() {
        return (Pyramid) getPlayer().getPartyQuest();
    }

    public void createExpedition(MapleExpeditionType type) {
        MapleExpedition exped = new MapleExpedition(getPlayer(), type);
        getPlayer().getClient().getChannelServer().getExpeditions().add(exped);
    }

    public void endExpedition(MapleExpedition exped) {
        exped.dispose();
        getPlayer().getClient().getChannelServer().getExpeditions().remove(exped);
    }

    public MapleExpedition getExpedition(MapleExpeditionType type) {
        for (MapleExpedition exped : getPlayer().getClient().getChannelServer().getExpeditions()) {
            if (exped.getType().equals(type)) {
                return exped;
            }
        }
        return null;
    }
}
