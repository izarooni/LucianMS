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
package com.lucianms.io.scripting.npc;

import com.lucianms.client.*;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.ItemFactory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MaplePet;
import com.lucianms.constants.ExpTable;
import com.lucianms.events.gm.MapleEvent;
import com.lucianms.io.scripting.AbstractPlayerInteraction;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.Server;
import com.lucianms.server.gachapon.MapleGachapon;
import com.lucianms.server.gachapon.MapleGachapon.MapleGachaponItem;
import com.lucianms.server.guild.MapleAlliance;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.partyquest.Pyramid;
import com.lucianms.server.partyquest.Pyramid.PyramidMode;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import tools.MaplePacketCreator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

    private int objectID;
    private int npc;
    private String scriptName;
    private String getText;

    public NPCConversationManager(MapleClient c, int objectID, int npc, String scriptName) {
        super(c);
        this.objectID = objectID;
        this.npc = npc;
        this.scriptName = scriptName;
    }

    public int getObjectID() {
        return objectID;
    }

    public int getNpc() {
        return npc;
    }

    public void setNpc(int npc) {
        this.npc = npc;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void dispose() {
        NPCScriptManager.dispose(this);
    }

    public void sendNext(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
    }

    public void sendPrev(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
    }

    public void sendNextPrev(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
    }

    public void sendOk(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public void sendYesNo(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
    }

    public void sendAcceptDecline(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", (byte) 0));
    }

    public void sendSimple(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, "", (byte) 0));
    }

    public void sendNext(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", speaker));
    }

    public void sendPrev(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", speaker));
    }

    public void sendNextPrev(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", speaker));
    }

    public void sendOk(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", speaker));
    }

    public void sendYesNo(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 1, text, "", speaker));
    }

    public void sendAcceptDecline(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", speaker));
    }

    public void sendSimple(String text, byte speaker) {
        getClient().announce(MaplePacketCreator.getNPCTalk(npc, (byte) 4, text, "", speaker));
    }

    public void sendStyle(String text, int styles[]) {
        getClient().announce(MaplePacketCreator.getNPCTalkStyle(npc, text, styles));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        getClient().announce(MaplePacketCreator.getNPCTalkNum(npc, text, def, min, max));
    }

    public void sendGetText(String text) {
        getClient().announce(MaplePacketCreator.getNPCTalkText(npc, text, ""));
    }

    /**
     * <ol start=0>
     * <li>ariant colliseum</li>
     * <li>Dojo</li>
     * <li>Carnival 1</li>
     * <li>Carnival 2</li>
     * <li>Ghost Ship PQ?</li>
     * <li>Pyramid PQ</li>
     * <li>Kerning Subway</li>
     * <ol/>
     */
    public void sendDimensionalMirror(String text) {
        getClient().announce(MaplePacketCreator.getDimensionalMirror(text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return this.getText;
    }

    public void showAnimation(byte action) {
        getClient().announce(MaplePacketCreator.getNpcAction(objectID, action));
    }

    public int getJobId() {
        return getPlayer().getJob().getId();
    }

    public MapleJob getJob() {
        return getPlayer().getJob();
    }

    public boolean isQuestStarted(short id) {
        return getPlayer().getQuest(MapleQuest.getInstance(id)).getStatus() == MapleQuestStatus.Status.STARTED;
    }

    public void startQuest(short id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } catch (NullPointerException ignored) {
        }
    }

    public void completeQuest(short id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } catch (NullPointerException ignored) {
        }
    }

    public void startQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceStart(getPlayer(), npc);
        } catch (NullPointerException ignored) {
        }
    }

    public void completeQuest(int id) {
        try {
            MapleQuest.getInstance(id).forceComplete(getPlayer(), npc);
        } catch (NullPointerException ignored) {
        }
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainMeso(int gain) {
        getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(int gain) {
        getPlayer().gainExp(gain, true, true);
    }

    public int getLevel() {
        return getPlayer().getLevel();
    }

    public void showEffect(String effect) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(effect, 3));
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor(MapleSkinColor.getById(color));
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int itemQuantity(int itemid) {
        return getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(itemid)).countById(itemid);
    }

    public void displayGuildRanks() {
        MapleGuild.displayGuildRanks(getClient(), npc);
    }

    public void gainCloseness(int closeness) {
        for (MaplePet pet : getPlayer().getPets()) {
            if (pet.getCloseness() > 30000) {
                pet.setCloseness(30000);
                return;
            }
            pet.gainCloseness(closeness);
            while (pet.getCloseness() > ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
                pet.setLevel((byte) (pet.getLevel() + 1));
                byte index = getPlayer().getPetIndex(pet);
                getClient().announce(MaplePacketCreator.showOwnPetLevelUp(index));
                getPlayer().getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showPetLevelUp(getPlayer(), index));
            }
            Item petz = getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
            getPlayer().forceUpdateItem(petz);
        }
    }

    public String getName() {
        return getPlayer().getName();
    }

    public int getGender() {
        return getPlayer().getGender();
    }

    public void changeJobById(int a) {
        getPlayer().changeJob(MapleJob.getById(a));
    }

    public void changeJob(MapleJob job) {
        getPlayer().changeJob(job);
    }

    public MapleJob getJobName(int id) {
        return MapleJob.getById(id);
    }

    public MapleStatEffect getItemEffect(int itemId) {
        return MapleItemInformationProvider.getInstance().getItemEffect(itemId);
    }

    public void resetStats() {
        getPlayer().resetStats();
    }

    public void maxMastery() {
        for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                getPlayer().changeSkillLevel(skill, (byte) 0, skill.getMaxLevel(), -1);
            } catch (NumberFormatException nfe) {
                break;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public void doGachapon() {
        int[] maps = {100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000};
        MapleGachaponItem item = MapleGachapon.getInstance().process(npc);
        Item itemGained = gainItem(item.getId(), (short) (item.getId() / 10000 == 200 ? 100 : 1), true, true); // For normal potions, make it give 100.
        sendNext("You have obtained a #b#t" + item.getId() + "##k.");
        String map = c.getChannelServer().getMap(maps[(getNpc() != 9100117 && getNpc() != 9100109) ? (getNpc() - 9100100) : getNpc() == 9100109 ? 8 : 9]).getMapName();
        if (item.getTier() > 0) { //Uncommon and Rare
            Server.broadcastMessage(MaplePacketCreator.gachaponMessage(itemGained, map, getPlayer()));
        }
    }

    public void disbandAlliance(MapleClient c, int allianceId) {
        Server.allianceMessage(c.getPlayer().getGuild().getAllianceId(), MaplePacketCreator.disbandAlliance(allianceId), -1, -1);
        Server.disbandAlliance(allianceId);
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM `alliance` WHERE id = ?")) {
            ps.setInt(1, allianceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean canBeUsedAllianceName(String name) {
        if (name.isEmpty() || name.contains(" ") || name.length() > 12) {
            return false;
        }
        try (Connection con = getClient().getWorldServer().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM alliance WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static MapleAlliance createAlliance(MapleCharacter chr1, MapleCharacter chr2, String name) {
        int id;
        int guild1 = chr1.getGuildId();
        int guild2 = chr2.getGuildId();
        try (Connection con = chr1.getClient().getWorldServer().getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                id = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        MapleAlliance alliance = new MapleAlliance(name, id, guild1, guild2);
        Server.setGuildAllianceId(guild1, id);
        Server.setGuildAllianceId(guild2, id);
        chr1.setAllianceRank(1);
        chr1.saveGuildStatus();
        chr2.setAllianceRank(2);
        chr2.saveGuildStatus();
        Server.addAlliance(id, alliance);
        Server.allianceMessage(id, MaplePacketCreator.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
        return alliance;
    }

    public boolean hasMerchant() {
        return getPlayer().hasMerchant();
    }

    public boolean hasMerchantItems() {
        try (Connection con = getClient().getWorldServer().getConnection()) {
            if (!ItemFactory.MERCHANT.loadItems(con, getPlayer().getId(), false).isEmpty()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return getPlayer().getMerchantMeso() != 0;
    }

    public void showFredrick() {
        c.announce(MaplePacketCreator.getFredrick(getPlayer()));
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public MapleEvent getEvent() {
        return c.getChannelServer().getEvent();
    }

    public void divideTeams() {
        if (getEvent() != null) {
            getPlayer().setTeam(getEvent().getLimit() % 2); //muhaha :D
        }
    }

    public MapleCharacter getMapleCharacter(String username) {
        return getClient().getChannelServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
    }

    public boolean createPyramid(String mode, boolean party) {//lol
        PyramidMode mod = PyramidMode.valueOf(mode);

        MapleParty partyz = getPlayer().getParty();

        MapleMap map = null;
        int mapid = 926010100;
        if (party) {
            mapid += 10000;
        }
        mapid += (mod.getMode() * 1000);

        for (byte b = 0; b < 5; b++) {//They cannot warp to the next map before the timer ends (:
            map = c.getChannelServer().getMap(mapid + b);
            if (map.getCharacters().isEmpty()) {
                break;
            }
        }

        if (!party) {
            partyz = new MapleParty(-1, new MaplePartyCharacter(getPlayer()));
        }
        Pyramid py = new Pyramid(partyz, mod, map.getId());
        getPlayer().setPartyQuest(py);
        py.warp(mapid);
        dispose();
        return true;
    }
}
