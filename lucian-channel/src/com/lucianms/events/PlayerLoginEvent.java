package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.PlayerBuffValueHolder;
import com.lucianms.server.Server;
import com.lucianms.server.channel.CharacterIdChannelPair;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleAlliance;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.world.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * @author izarooni
 */
public class PlayerLoginEvent extends PacketEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerLoginEvent.class);

    private int playerID;

    @Override
    public boolean inValidState() {
        return getClient().getLoginState() == LoginState.LogOut;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getWorldServer().getPlayer(playerID);
        if (player == null) {
            try (Connection con = getClient().getWorldServer().getConnection()) {
                player = MapleCharacter.loadCharFromDB(con, playerID, getClient(), true);
            } catch (SQLException e) {
                getLogger().error("Unable to load player '{}", MapleCharacter.getNameById(playerID), e);
                getClient().getSession().close();
                return null;
            }
        } else {
            player.newClient(getClient());
        }
        getClient().setAccID(player.getAccountID());
        getClient().setPlayer(player);

        final LoginState state = getClient().checkLoginState();
        MapleChannel channel = getClient().getChannelServer();

        if (getClient().getAccID() == 0 || state != LoginState.Transfer) {
            getClient().setPlayer(null);
            getClient().announce(MaplePacketCreator.getAfterLoginError(7));
            return null;
        }
        for (Pair<Integer, String> p : getClient().loadCharactersInternal(getClient().getWorld())) {
            for (MapleChannel ch : getClient().getWorldServer().getChannels()) {
                MapleCharacter found = ch.getPlayerStorage().get(p.getLeft());
                if (found != null) {
                    found.getClient().disconnect();
                    break;
                }
            }
        }
        getClient().setLoginState(LoginState.Login);

        if (JailManager.isJailed(player.getId())) {
            player.setMapId(JailManager.getRandomField());
        }

        channel.addPlayer(player);
        List<PlayerBuffValueHolder> buffs = Server.getPlayerBuffStorage().remove(playerID);
        if (buffs != null) {
            player.silentGiveBuffs(buffs);
            buffs.clear();
        }
        try (Connection con = getClient().getWorldServer().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE RecieverId = ? AND Checked = 1")) {
                ps.setInt(1, player.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement pss = con.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE RecieverId = ?")) {
                            pss.setInt(1, player.getId());
                            pss.executeUpdate();
                        }
                        getClient().announce(MaplePacketCreator.sendDueyMSG((byte) 0x1B));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (player.isGM() && !player.isHidden()) {
            player.setHidingLevel(player.getGMLevel());
            player.setHide(true);
        }

        getClient().announce(MaplePacketCreator.getCharInfo(player));
        getClient().announce(MaplePacketCreator.updateGender((byte) player.getGender()));
        getClient().announce(MaplePacketCreator.enableReport());
        player.getMap().addPlayer(player);
        player.sendKeymap();
        player.sendMacros();

        if (player.getKeymap().get(91) != null) {
            player.announce(MaplePacketCreator.sendAutoHpPot(player.getKeymap().get(91).getAction()));
        }
        if (player.getKeymap().get(92) != null) {
            player.announce(MaplePacketCreator.sendAutoMpPot(player.getKeymap().get(92).getAction()));
        }

        MapleWorld world = getClient().getWorldServer();

        //region friends list
        int[] buddyIds = player.getBuddylist().getBuddyIds();
        world.loggedOn(player.getName(), player.getId(), getClient().getChannel(), buddyIds);
        for (CharacterIdChannelPair onlineBuddy : world.multiBuddyFind(player.getId(), buddyIds)) {
            BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
            ble.setChannel(onlineBuddy.getChannel());
            player.getBuddylist().put(ble);
        }
        getClient().announce(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

        CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            getClient().announce(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), player.getId(), pendingBuddyRequest.getName()));
        }
        //endregion

        //region family
        getClient().announce(MaplePacketCreator.loadFamily(player));
        if (player.getFamilyId() > 0) {
            MapleFamily f = world.getFamily(player.getFamilyId());
            if (f == null) {
                f = new MapleFamily(player.getId());
                world.addFamily(player.getFamilyId(), f);
            }
            player.setFamily(f);
            getClient().announce(MaplePacketCreator.getFamilyInfo(f.getMember(player.getId())));
        }
        //endregion

        //region guild
        if (player.getGuildId() > 0) {
            MapleGuild playerGuild = Server.getGuild(player.getGuildId(), player.getWorld(), player.getMGC());
            if (playerGuild == null) {
                player.deleteGuild(player.getGuildId());
                player.resetMGC();
                player.setGuildId(0);
            } else {
                Server.setGuildMemberOnline(player.getMGC(), true, getClient().getChannel());
                getClient().announce(MaplePacketCreator.showGuildInfo(player));
                int allianceId = player.getGuild().getAllianceId();
                if (allianceId > 0) {
                    MapleAlliance newAlliance = Server.getAlliance(allianceId);
                    if (newAlliance == null) {
                        newAlliance = MapleAlliance.loadAlliance(allianceId);
                        if (newAlliance != null) {
                            Server.addAlliance(allianceId, newAlliance);
                        } else {
                            player.getGuild().setAllianceId(0);
                        }
                    }
                    if (newAlliance != null) {
                        getClient().announce(MaplePacketCreator.getAllianceInfo(newAlliance));
                        getClient().announce(MaplePacketCreator.getGuildAlliances(newAlliance, getClient()));
                        Server.allianceMessage(allianceId, MaplePacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
                    }
                }
            }
        }
        //endregion

        //region party
        MapleParty party = player.getParty();
        if (party != null) {
            MaplePartyCharacter me = party.get(player.getId());
            me.updateWithPlayer(player);
            Collection<MapleCharacter> members = party.getPlayers();
            for (MapleCharacter member : members) {
                member.announce(MaplePacketCreator.updateParty(member.getClient().getChannel(), party, PartyOperation.SILENT_UPDATE, null));
            }
            members.clear();
            player.receivePartyMemberHP();
            player.updatePartyMemberHP();
        }
        //endregion

        MapleInventory eqd = player.getInventory(MapleInventoryType.EQUIPPED);
        if (eqd.findById(1122017) != null) {
            player.scheduleSpiritPendant();
        }

        player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 12), (byte) (player.getLinkedLevel() / 10), 20, -1);
        player.changeSkillLevel(SkillFactory.getSkill(5001005), (byte) 0, 0, 0);
        player.changeSkillLevel(SkillFactory.getSkill(15001003), (byte) 0, 0, 0);

        MapleMessenger messenger = player.getMessenger();
        if (messenger != null) {
            MapleMessengerCharacter member = messenger.get(player.getId());
            if (member != null) {
                member.setPlayer(player);
                member.updateWithPlayer(player);
                messenger.sendPacket(MaplePacketCreator.updateMessengerPlayer(member), player);
            }
        }

        player.showNote();
        player.checkBerserk();
        player.expirationTask();
        player.setRates();
        Achievements.testFor(player, -1);
        return null;
    }
}
