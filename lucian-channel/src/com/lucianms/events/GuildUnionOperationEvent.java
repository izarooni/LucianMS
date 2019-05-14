package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import com.lucianms.server.guild.MapleAlliance;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author XoticStory
 * @author izarooni
 */
public class GuildUnionOperationEvent extends PacketEvent {

    private String username;
    private String name;
    private String content;
    private String[] titles;
    private byte action;
    private byte unkb1;
    private int guildID;
    private int allianceID;
    private int playerID;
    private int unki1;

    @Override
    public void clean() {
        titles = null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 3:
                username = reader.readMapleAsciiString();
                name = reader.readMapleAsciiString();
                break;
            case 4:
                guildID = reader.readInt();
                break;
            case 6:
                guildID = reader.readInt();
                allianceID = reader.readInt();
                break;
            case 7:
                playerID = reader.readInt();
                break;
            case 8:
                titles = new String[5];
                for (int i = 0; i < 5; i++) {
                    titles[i] = reader.readMapleAsciiString();
                }
                break;
            case 9:
                unki1 = reader.readInt();
                unkb1 = reader.readByte();
                break;
            case 0xA:
                content = reader.readMapleAsciiString();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleAlliance alliance = null;
        if (player.getGuild() != null && player.getGuild().getAllianceId() > 0) {
            alliance = Server.getAlliance(player.getGuild().getAllianceId());
        }
        if (alliance == null) {
            player.dropMessage("You are not in an alliance.");
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        } else if (player.getMGC().getAllianceRank() > 2 || !alliance.getGuilds().contains(player.getGuildId())) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        switch (action) {
            case 0x01:
                Server.allianceMessage(alliance.getId(), sendShowInfo(player.getGuild().getAllianceId(), player.getId()), -1, -1);
                break;
            case 0x02: { // Leave Alliance
                if (player.getGuild().getAllianceId() == 0 || player.getGuildId() < 1 || player.getGuildRank() != 1) {
                    return null;
                }
                Server.allianceMessage(alliance.getId(), sendChangeGuild(player.getGuildId(), player.getId(), player.getGuildId(), 2), -1, -1);
                break;
            }
            case 0x03: {// send alliance invite
                MapleCharacter target = getClient().getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
                if (target == null) {
                    player.dropMessage("The player is not online.");
                } else {
                    if (target.getGuildId() == 0) {
                        player.dropMessage("The person you are trying to invite does not have a guild.");
                    } else if (target.getGuildRank() != 1) {
                        player.dropMessage("The player is not the leader of his/her guild.");
                    } else {
                        Server.allianceMessage(alliance.getId(), sendInvitation(player.getGuild().getAllianceId(), player.getId(), name), -1, -1);
                    }
                }
                break;
            }
            case 0x04: {
                if (player.getGuild().getAllianceId() != 0 || player.getGuildRank() != 1 || player.getGuildId() < 1) {
                    return null;
                }
                Server.allianceMessage(alliance.getId(), sendChangeGuild(guildID, player.getId(), player.getGuildId(), 0), -1, -1);
                break;
            }
            case 0x06: { // Expel Guild
                if (player.getGuild().getAllianceId() == 0 || player.getGuild().getAllianceId() != allianceID) {
                    return null;
                }
                Server.allianceMessage(alliance.getId(), sendChangeGuild(allianceID, player.getId(), guildID, 1), -1, -1);
                break;
            }
            case 0x07: { // Change Alliance Leader
                if (player.getGuild().getAllianceId() == 0 || player.getGuildId() < 1) {
                    return null;
                }
                Server.allianceMessage(alliance.getId(), sendChangeLeader(player.getGuild().getAllianceId(), player.getId(), playerID), -1, -1);
                break;
            }
            case 0x08:
                Server.setAllianceRanks(alliance.getId(), titles);
                Server.allianceMessage(alliance.getId(), MaplePacketCreator.changeAllianceRankTitle(alliance.getId(), titles), -1, -1);
                break;
            case 0x09: {
                Server.allianceMessage(alliance.getId(), sendChangeRank(player.getGuild().getAllianceId(), player.getId(), unki1, unkb1), -1, -1);
                break;
            }
            case 0x0A:
                Server.setAllianceNotice(alliance.getId(), content);
                Server.allianceMessage(alliance.getId(), MaplePacketCreator.allianceNotice(alliance.getId(), content), -1, -1);
                break;
            default:
                player.dropMessage("Feature not available");
        }
        alliance.saveToDB();
        return null;
    }

    private static byte[] sendShowInfo(int allianceid, int playerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x02);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        return mplew.getPacket();
    }

    private static byte[] sendInvitation(int allianceid, int playerid, final String guildname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeMapleAsciiString(guildname);
        return mplew.getPacket();
    }

    private static byte[] sendChangeGuild(int allianceid, int playerid, int guildid, int option) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x07);
        mplew.writeInt(allianceid);
        mplew.writeInt(guildid);
        mplew.writeInt(playerid);
        mplew.write(option);
        return mplew.getPacket();
    }

    private static byte[] sendChangeLeader(int allianceid, int playerid, int victim) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x08);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeInt(victim);
        return mplew.getPacket();
    }

    private static byte[] sendChangeRank(int allianceid, int playerid, int int1, byte byte1) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x09);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeInt(int1);
        mplew.writeInt(byte1);
        return mplew.getPacket();
    }
}
