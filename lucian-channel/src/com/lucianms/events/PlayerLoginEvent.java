package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.helpers.JailManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.PlayerBuffValueHolder;
import com.lucianms.server.Server;
import com.lucianms.server.channel.CharacterIdChannelPair;
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
        MapleClient client = getClient();
        MapleWorld world = client.getWorldServer();

        MapleCharacter player;
        try (Connection con = world.getConnection()) {
            player = MapleCharacter.loadCharFromDB(con, playerID, client, true);
        } catch (SQLException e) {
            getLogger().error("Unable to load player '{}", MapleCharacter.getNameById(playerID), e);
            client.getSession().close();
            return null;
        }
        client.setAccID(player.getAccountID());
        client.setPlayer(player);

        final LoginState state = client.checkLoginState();

        if (client.getAccID() == 0 || state != LoginState.Transfer) {
            client.setPlayer(null);
            client.announce(MaplePacketCreator.getAfterLoginError(7));
            return null;
        }
        for (Pair<Integer, String> p : client.getCharacterIdentifiers()) {
            MapleCharacter found = world.getPlayerStorage().get(p.getLeft());
            if (found != null) {
                // to prevent any packet handlers i suppose
                found.getClient().setLoginState(LoginState.LogOut);
                found.getClient().announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, "You are being disconnected due to your account being logged-in from another location.", "00 00", (byte) 1));
                TaskExecutor.createTask(() -> found.getClient().dispose(), 6500);
            }
        }
        client.updateLoginState(LoginState.Login);

        if (JailManager.isJailed(player.getId())) {
            player.setMapId(JailManager.getRandomField());
        }
        try (Connection con = world.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE RecieverId = ? AND Checked = 1")) {
                ps.setInt(1, player.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement pss = con.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE RecieverId = ?")) {
                            pss.setInt(1, player.getId());
                            pss.executeUpdate();
                        }
                        client.announce(MaplePacketCreator.sendDueyMSG((byte) 0x1B));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        client.announce(MaplePacketCreator.getCharInfo(player));
        client.announce(MaplePacketCreator.updateGender((byte) player.getGender()));
        client.announce(MaplePacketCreator.enableReport());
        player.sendKeymap();
        player.sendMacros();

        if (player.isGM()) {
            player.setHidingLevel(player.getGMLevel());
            player.setHidden(true);
        }
        world.getPlayerStorage().put(player.getId(), player);
        player.getMap().addPlayer(player);

        List<PlayerBuffValueHolder> buffs = Server.getPlayerBuffStorage().remove(playerID);
        if (buffs != null) {
            player.silentGiveBuffs(buffs);
            buffs.clear();
        }

        if (player.getKeymap().get(91) != null) {
            player.announce(MaplePacketCreator.sendAutoHpPot(player.getKeymap().get(91).getAction()));
        }
        if (player.getKeymap().get(92) != null) {
            player.announce(MaplePacketCreator.sendAutoMpPot(player.getKeymap().get(92).getAction()));
        }

        //region friends list
        int[] buddyIds = player.getBuddylist().getBuddyIds();
        world.loggedOn(player.getName(), player.getId(), client.getChannel(), buddyIds);
        for (CharacterIdChannelPair onlineBuddy : world.multiBuddyFind(player.getId(), buddyIds)) {
            BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
            ble.setChannel(onlineBuddy.getChannel());
            player.getBuddylist().put(ble);
        }
        client.announce(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

        CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            client.announce(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), player.getId(), pendingBuddyRequest.getName()));
        }
        //endregion

        //region family
        client.announce(MaplePacketCreator.loadFamily(player));
        if (player.getFamilyId() > 0) {
            MapleFamily f = world.getFamily(player.getFamilyId());
            if (f == null) {
                f = new MapleFamily(player.getId());
                world.addFamily(player.getFamilyId(), f);
            }
            player.setFamily(f);
            client.announce(MaplePacketCreator.getFamilyInfo(f.getMember(player.getId())));
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
                Server.setGuildMemberOnline(player.getMGC(), true, client.getChannel());
                client.announce(MaplePacketCreator.showGuildInfo(player));
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
                        client.announce(MaplePacketCreator.getAllianceInfo(newAlliance));
                        client.announce(MaplePacketCreator.getGuildAlliances(newAlliance, client));
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

        MapleMessenger messenger = player.getMessenger();
        if (messenger != null) {
            MapleMessengerCharacter member = messenger.get(player.getId());
            if (member != null) {
                member.setPlayer(player);
                member.updateWithPlayer(player);
                messenger.sendPacketExclude(MaplePacketCreator.updateMessengerPlayer(member), player);
            }
        }

        byte skillBlessingLevel = (byte) (player.getLinkedLevel() / 10);
        if (skillBlessingLevel > 0) {
            int blessingSkillID = 10000000 * player.getJobType() + 12;
            Skill blessingSkill = SkillFactory.getSkill(blessingSkillID);
            if (blessingSkill != null) {
                player.changeSkillLevel(blessingSkillID, skillBlessingLevel, 20, -1);
            }
        }
        player.changeSkillLevel(5001005, (byte) -1, 0, 0);
        player.changeSkillLevel(15001003, (byte) -1, 0, 0);

        client.announce(MaplePacketCreator.serverMessage(world.getServerMessage()));

        player.showNote();
        player.checkBerserk();
        player.setRates();
        return null;
    }
}
