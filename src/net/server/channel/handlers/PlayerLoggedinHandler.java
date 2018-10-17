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

import client.*;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import net.AbstractMaplePacketHandler;
import net.server.PlayerBuffValueHolder;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.CharacterIdChannelPair;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class PlayerLoggedinHandler extends AbstractMaplePacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerLoggedinHandler.class);

    @Override
    public final boolean validateState(MapleClient c) {
        return !c.isLoggedIn();
    }

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final int cid = slea.readInt();
        final Server server = Server.getInstance();
        MapleCharacter player = c.getWorldServer().getPlayerStorage().getCharacterById(cid);
        boolean newcomer = false;
        if (player == null) {
            try (Connection con = c.getChannelServer().getConnection()) {
                player = MapleCharacter.loadCharFromDB(con, cid, c, true);
                newcomer = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            player.newClient(c);
        }
        if (player == null) {
            c.disconnect(true, false);
            return;
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());

        final int state = c.getLoginState();
        boolean allowLogin = true;
        Channel cserv = c.getChannelServer();

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            for (String charName : c.loadCharacterNames(c.getWorld())) {
                for (Channel ch : c.getWorldServer().getChannels()) {
                    if (ch.isConnected(charName)) {
                        allowLogin = false;
                        break;
                    }
                }
            }
        }
        if (state != MapleClient.LOGIN_SERVER_TRANSITION || !allowLogin) {
            c.setPlayer(null);
            c.announce(MaplePacketCreator.getAfterLoginError(7));
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN);

        if (JailManager.isJailed(player.getId())) {
            player.setMapId(JailManager.getRandomField());
        }

        LOGGER.info("Player {} logged-in", MapleCharacter.getNameById(cid));

        MapleInventory eqd = player.getInventory(MapleInventoryType.EQUIPPED);
//        if (eqd.getItem((short) -149) == null) {
//            Equip eq = new Equip(1802056, (short) -149);
//            player.getInventory(MapleInventoryType.EQUIPPED).addFromDB(eq);
//        }

        cserv.addPlayer(player);
        List<PlayerBuffValueHolder> buffs = server.getPlayerBuffStorage().getBuffsFromStorage(cid);
        if (buffs != null) {
            player.silentGiveBuffs(buffs);
        }
        try (Connection con = c.getChannelServer().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE RecieverId = ? AND Checked = 1")) {
                ps.setInt(1, player.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement pss = con.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE RecieverId = ?")) {
                            pss.setInt(1, player.getId());
                            pss.executeUpdate();
                        }
                        c.announce(MaplePacketCreator.sendDueyMSG((byte) 0x1B));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        c.announce(MaplePacketCreator.getCharInfo(player));
        player.sendKeymap();
        player.sendMacros();

        if (player.getKeymap().get(91) != null) {
            player.announce(MaplePacketCreator.sendAutoHpPot(player.getKeymap().get(91).getAction()));
        }
        if (player.getKeymap().get(92) != null) {
            player.announce(MaplePacketCreator.sendAutoMpPot(player.getKeymap().get(92).getAction()));
        }

        player.getMap().addPlayer(player);
        World world = server.getWorld(c.getWorld());
        world.getPlayerStorage().addPlayer(player);

        int buddyIds[] = player.getBuddylist().getBuddyIds();
        world.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
        for (CharacterIdChannelPair onlineBuddy : server.getWorld(c.getWorld()).multiBuddyFind(player.getId(), buddyIds)) {
            BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
            ble.setChannel(onlineBuddy.getChannel());
            player.getBuddylist().put(ble);
        }
        c.announce(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
        c.announce(MaplePacketCreator.loadFamily(player));
        if (player.getFamilyId() > 0) {
            MapleFamily f = world.getFamily(player.getFamilyId());
            if (f == null) {
                f = new MapleFamily(player.getId());
                world.addFamily(player.getFamilyId(), f);
            }
            player.setFamily(f);
            c.announce(MaplePacketCreator.getFamilyInfo(f.getMember(player.getId())));
        }
        if (player.getGuildId() > 0) {
            MapleGuild playerGuild = server.getGuild(player.getGuildId(), player.getWorld(), player.getMGC());
            if (playerGuild == null) {
                player.deleteGuild(player.getGuildId());
                player.resetMGC();
                player.setGuildId(0);
            } else {
                server.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.announce(MaplePacketCreator.showGuildInfo(player));
                int allianceId = player.getGuild().getAllianceId();
                if (allianceId > 0) {
                    MapleAlliance newAlliance = server.getAlliance(allianceId);
                    if (newAlliance == null) {
                        newAlliance = MapleAlliance.loadAlliance(allianceId);
                        if (newAlliance != null) {
                            server.addAlliance(allianceId, newAlliance);
                        } else {
                            player.getGuild().setAllianceId(0);
                        }
                    }
                    if (newAlliance != null) {
                        c.announce(MaplePacketCreator.getAllianceInfo(newAlliance));
                        c.announce(MaplePacketCreator.getGuildAlliances(newAlliance, c));
                        server.allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
                    }
                }
            }
        }
        player.showNote();
        if (player.getParty() != null) {
            MaplePartyCharacter pchar = player.getMPC();
            pchar.setChannel(c.getChannel());
            pchar.setMapId(player.getMapId());
            pchar.setOnline(true);
            world.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, pchar);
        }
        player.updatePartyMemberHP();

        if (eqd.findById(1122017) != null) {
            player.scheduleSpiritPendant();
        }
        c.announce(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.announce(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }

        c.announce(MaplePacketCreator.updateGender(player));
        player.checkMessenger();
        c.announce(MaplePacketCreator.enableReport());
        player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 12), (byte) (player.getLinkedLevel() / 10), 20, -1);
        player.checkBerserk();
        player.expirationTask();
        player.setRates();
        if (newcomer && !c.hasVotedAlready()) {
            player.announce(MaplePacketCreator.earnTitleMessage("You can vote now! Vote and earn a vote point!"));
        }

        NPCScriptManager.start(c, 2007, "f_daily_login");

        Achievements.testFor(player, -1);
    }
}
