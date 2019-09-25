package com.lucianms.features.carnival;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;

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

        final MaplePacketWriter w = new MaplePacketWriter(25);
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_START.getValue());
        w.write(player.getTeam()); // team
        w.writeShort(player.getCP()); // Current points
        w.writeShort(player.getObtainedCP()); // Total points

        w.writeShort(friendly.getAvailableCarnivalPoints()); // Friendly team's current points
        w.writeShort(friendly.getTotalCarnivalPoints()); // Friendly team's total points
        w.writeShort(enemy.getAvailableCarnivalPoints()); // Enemy team's current points
        w.writeShort(enemy.getTotalCarnivalPoints()); // Enemy team's total points

        w.writeShort(0);
        w.writeLong(0);
        return w.getPacket();
    }

    public static byte[] getMonsterCarnivalPointsUpdate(int available, int total) {
        final MaplePacketWriter w = new MaplePacketWriter(6);
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        w.writeShort(available); // Current points
        w.writeShort(total); // Total points
        return w.getPacket();
    }

    public static byte[] getMonsterCarnivalPointsUpdateParty(MCarnivalTeam team) {
        final MaplePacketWriter w = new MaplePacketWriter(7);
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
        w.write(team.getId()); // Belonging team
        w.writeShort(team.getAvailableCarnivalPoints()); // Team's current points
        w.writeShort(team.getTotalCarnivalPoints()); // Team's total points
        return w.getPacket();
    }

    public static byte[] getMonsterCarnivalSummon(int tab, int number, String name) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        w.write(tab); // Tab
        w.write(number); // Summon index
        w.writeMapleString(name); // Name of summoner
        return w.getPacket();
    }

    public static byte[] getMonsterCarnivalPlayerDeath(MapleCharacter player) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_DIED.getValue());
        w.write(player.getTeam());
        w.writeMapleString(player.getName());
        w.write(player.getAndRemoveCP());
        return w.getPacket();
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
        final MaplePacketWriter w = new MaplePacketWriter(3);
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        w.write(message);
        return w.getPacket();
    }

    public static byte[] getMonsterCarnivalStop(byte team, boolean isLeader, String username) {
        final MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        w.write((isLeader) ? 6 : 0); // v1 = CInPacket::Decode1(a1) == 6
        w.write(team);
        w.writeMapleString(username);
        return w.getPacket();
    }
}
