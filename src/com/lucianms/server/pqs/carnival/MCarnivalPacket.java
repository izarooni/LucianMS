package com.lucianms.server.pqs.carnival;

import client.MapleCharacter;
import net.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * <p>
 * IDA memory address: {@code 005652EE}
 * </p>
 *
 * @author izarooni
 */
public final class MCarnivalPacket {

    private MCarnivalPacket() {
    }

    /**
     * <p>
     * Should only be sent to a user once in a single Monster Carnival instance
     * otherwise rows will duplicate in the summon list
     * </p>
     *
     * @param player       the player receiving the packet
     * @param carnivalGame the carnival game the player is registered to
     */
    public static byte[] getMonsterCarnivalStart(MapleCharacter player, MCarnivalGame carnivalGame) {
        MCarnivalTeam friendly = carnivalGame.getTeam(player.getTeam());
        MCarnivalTeam enemy = carnivalGame.getTeamOpposite(player.getTeam());

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(25);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_START.getValue());
        mplew.write(player.getTeam()); // team
        mplew.writeShort(player.getCP()); // Current points
        mplew.writeShort(player.getObtainedCP()); // Total points

        mplew.writeShort(friendly.getAvailableCarnivalPoints()); // Friendly team's current points
        mplew.writeShort(friendly.getTotalCarnivalPoints()); // Friendly team's total points
        mplew.writeShort(enemy.getAvailableCarnivalPoints()); // Enemy team's current points
        mplew.writeShort(enemy.getTotalCarnivalPoints()); // Enemy team's total points

        mplew.writeShort(0);
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] getMonsterCarnivalPointsUpdate(int available, int total) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        mplew.writeShort(available); // Current points
        mplew.writeShort(total); // Total points
        return mplew.getPacket();
    }

    public static byte[] getMonsterCarnivalPointsUpdateParty(MCarnivalTeam team) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
        mplew.write(team.getId()); // Belonging team
        mplew.writeShort(team.getAvailableCarnivalPoints()); // Team's current points
        mplew.writeShort(team.getTotalCarnivalPoints()); // Team's total points
        return mplew.getPacket();
    }

    public static byte[] getMonsterCarnivalSummon(int tab, int number, String name) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(tab); // Tab
        mplew.writeShort(number); // Summon index
        mplew.writeMapleAsciiString(name); // Name of summoner
        return mplew.getPacket();
    }

    public static byte[] getMonsterCarnivalPlayerDeath(MapleCharacter player) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_DIED.getValue());
        mplew.write(player.getTeam());
        mplew.writeMapleAsciiString(player.getName());
        mplew.write(player.getAndRemoveCP());
        return mplew.getPacket();
    }

    /**
     * <p>Possible values for the {@code message} parameter</p>
     * <ol start="1">
     * <li>You don't have enough CP to continue.</li>
     * <li>You can no longer summon the Monster.</li>
     * <li>You can no longer summon the being.</li>
     * <li>This being is already summoned.</li>
     * <li>This request has failed due to an unknown error.</li>
     * </ol>
     */
    public static byte[] getMonsterCarnivalResponse(byte message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    public static byte[] getMonsterCarnivalStop(byte team, boolean isLeader, String username) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        mplew.write((isLeader) ? 6 : 0); // v1 = CInPacket::Decode1(a1) == 6
        mplew.write(team);
        mplew.writeMapleAsciiString(username);
        return mplew.getPacket();
    }
}
