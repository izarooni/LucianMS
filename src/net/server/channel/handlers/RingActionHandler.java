package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.Relationship;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import net.SendOpcode;
import net.server.channel.Channel;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author izarooni
 */
public class RingActionHandler extends AbstractMaplePacketHandler {

    private static final HashMap<Integer, Triple<Integer, Integer, Integer>> Boxes = new HashMap<>();

    static {
        // format: engagement box, Pair(empty engagement box, engagement ring, wedding ring)
        Boxes.put(2240000, new Triple<>(4031357, 4031358, 1112803)); // moonstone
        Boxes.put(2240001, new Triple<>(4031359, 4031370, 1112806)); // star gem
        Boxes.put(2240002, new Triple<>(4031361, 4031362, 1112807)); // golden heart
        Boxes.put(2240003, new Triple<>(4031363, 4031364, 1112809)); // silver swan
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        Channel ch = client.getChannelServer();

        MapleCharacter player = client.getPlayer();
        Relationship rltn = player.getRelationship();

        MapleCharacter pplayer = null; // partner player
        Relationship prltn = null; // partner relationship
        int partnerId = (rltn.getGroomId() == player.getId()) ? rltn.getBrideId() : rltn.getGroomId();
        if (partnerId > 0) {
            pplayer = client.getWorldServer().getPlayerStorage().getCharacterById(partnerId);
            if (pplayer != null) {
                prltn = pplayer.getRelationship();
            }
        }

        byte action = slea.readByte();
        switch (action) {
            case 0: { // proposal
                String tUsername = slea.readMapleAsciiString();
                int itemId = slea.readInt();
                if (player.getGender() != 0) { // only males are able to propose
                    client.announce(getEngagementResult((byte) 0x13)); // any error message really
                } else if (tUsername.equalsIgnoreCase(player.getName())) {
                    client.announce(getEngagementResult((byte) 0x12));
                } else if (rltn.getStatus() == Relationship.Status.Single && rltn.getBrideId() > 0) {
                    client.announce(getEngagementResult((byte) 0x1b));
                } else {
                    if (!Boxes.containsKey(itemId)) {
                        logger().warn("'{}' attempting to propose with a non-engagement box item ({})", player.getName(), itemId);
                        return;
                    } else if (player.getInventory(MapleInventoryType.USE).findById(itemId) == null) {
                        logger().warn("'{}' attempting to propose with an invalid item ({})", player.getName(), itemId);
                        return;
                    }
                    pplayer = ch.getPlayerStorage().getCharacterByName(tUsername);
                    if (pplayer != null) {
                        prltn = pplayer.getRelationship();
                        if (pplayer.getMapId() != player.getMapId()) {
                            // Both players must be in the same map
                            client.announce(getEngagementResult((byte) 0x13));
                        } else if (pplayer.getGender() == player.getGender()) {
                            // Must be different genders  -- females can't interact with the box anyways
                            client.announce(getEngagementResult((byte) 0x16));
                        } else if (prltn.getStatus() == Relationship.Status.Married) {
                            // partner is already married
                            client.announce(getEngagementResult((byte) 0x18));
                        } else if (rltn.getStatus() == Relationship.Status.Married) {
                            // player is already married
                            client.announce(getEngagementResult((byte) 0x1a));
                        } else if (prltn.getStatus() == Relationship.Status.Engaged) {
                            // partner is already engaged
                            client.announce(getEngagementResult((byte) 0x19));
                        } else if (rltn.getStatus() == Relationship.Status.Engaged) {
                            // player is already engaged
                            client.announce(getEngagementResult((byte) 0x17));
                        } else if (prltn.getStatus() == Relationship.Status.Single && prltn.getGroomId() > 0) {
                            // partner is already being proposed to
                            client.announce(getEngagementResult((byte) 0x1c));
                        } else {
                            // holy finally
                            rltn.setEngagementBoxId(itemId);
                            prltn.setEngagementBoxId(itemId);

                            prltn.setBrideId(pplayer.getId());
                            prltn.setGroomId(player.getId());
                            rltn.setBrideId(pplayer.getId());
                            rltn.setGroomId(player.getId());

                            pplayer.announce(sendEngagementRequest(player.getName(), player.getId(), tUsername));
                        }
                    } else {
                        client.announce(getEngagementResult((byte) 0x12));
                    }
                }
                break;
            }
            case 1: { // request: canceled
                if (rltn.getGroomId() == 0 || rltn.getBrideId() == 0) {
                    // can't cancel a proposal if there isn't a proposal :thinking:
                    return;
                }
                resetRelationship(rltn);
                if (pplayer != null) {
                    pplayer.announce(getEngagementResult((byte) 0x1d));
                    resetRelationship(prltn);
                }
                break;
            }
            case 2: { // request: result
                boolean accepted = slea.readByte() == 1;
                String tUsername = slea.readMapleAsciiString();
                int groomId = slea.readInt();
                if (rltn.getStatus() != Relationship.Status.Single || rltn.getGroomId() == 0 || rltn.getBrideId() == 0) {
                    return;
                }
                if (groomId != rltn.getGroomId()) {
                    // attempting to engage with someone else
                    logger().warn("'{}' attempting to become engaged with somebody else ('{}': {}) other than proposed person ('{}')", player.getName(), tUsername, groomId, MapleCharacter.getNameById(rltn.getBrideId()));
                    return;
                }
                if (pplayer != null && pplayer.getRelationship().getStatus() == Relationship.Status.Single) {
                    if (accepted) {
                        if (pplayer.getInventory(MapleInventoryType.ETC).isFull()) {
                            pplayer.announce(MaplePacketCreator.getInventoryFull()); // not sure if this will work
                            player.announce(getEngagementResult((byte) 0x15));
                        } else if (player.getInventory(MapleInventoryType.ETC).isFull()) {
                            player.announce(MaplePacketCreator.getInventoryFull());
                            pplayer.announce(getEngagementResult((byte) 0x15));
                        } else {
                            Pair<Integer, Integer> p = getEngagementItems(rltn.getEngagementBoxId());
                            pplayer.announce(getEngagementSuccess(pplayer, player)); // do i need to send this to both players? HMMM
                            rltn.setStatus(Relationship.Status.Engaged);
                            prltn.setStatus(Relationship.Status.Engaged);
                            MapleInventoryManipulator.removeById(pplayer.getClient(), MapleInventoryType.USE, pplayer.getRelationship().getEngagementBoxId(), 1, false, false);
                            // male gets box and female gets ring :D
                            MapleInventoryManipulator.addById(pplayer.getClient(), p.getLeft(), (short) 1); // (empty) engagement box
                            MapleInventoryManipulator.addById(client, p.getRight(), (short) 1); // engagement ring

                            try {
                                rltn.save();
                            } catch (SQLException e) {
                                logger().warn("Unable to save relationship data for player '{}': {}", player.getName(), e.getMessage());
                            }
                        }
                    } else {
                        pplayer.announce(getEngagementResult((byte) 0x1e));

                        resetRelationship(rltn);
                        resetRelationship(prltn);
                    }
                }
                break;
            }
            case 3: { // drop ring / engagement box
                int itemId = slea.readInt();
                if (player.getInventory(MapleInventoryType.ETC).findById(itemId) == null) {
                    logger().warn("Attempting to drop null item ({})", itemId);
                    return;
                }
                Triple<Integer, Integer, Integer> box = Boxes.get(rltn.getEngagementBoxId());
                if (box == null || itemId != box.getLeft() && itemId != box.getMiddle()) {
                    logger().warn("'{}' dropped an invalid ring/box ({})", player.getName(), itemId);
                    return;
                }
                client.announce(getEngagementResult((byte) 0xd));
                MapleInventoryManipulator.removeById(client, MapleInventoryType.ETC, itemId, 1, false, false);
                resetRelationship(rltn);

                if (pplayer != null) {
                    pplayer.announce(getEngagementResult((byte) 0xd)); // am i supposed to send this to both players? hmmm
                    itemId = (itemId == box.getLeft() ? box.getRight() : box.getLeft());
                    MapleInventoryManipulator.removeById(pplayer.getClient(), MapleInventoryType.ETC, itemId, 1, false, false);
                    resetRelationship(prltn);
                }
                break;
            }
            case 6: { // wedding invitation
                short slot = slea.readShort();
                slea.skip(2);
                int itemId = slea.readInt();
                Item item = player.getInventory(MapleInventoryType.ETC).getItem(slot);
                if (item != null && item.getItemId() == itemId) {

                }
                break;
            }
            case 9: { // groom's wish list
                byte q = slea.readByte();
                for (int i = 0; i < Math.min(q, 10); i++) {
                    String entry = slea.readMapleAsciiString();
                }
                client.announce(MaplePacketCreator.sendGroomWishlist());
                break;
            }
            default: {
                logger().warn("Unhandled ring action {}", slea.toString());
                break;
            }
        }
    }

    public static Pair<Integer, Integer> getEngagementItems(int engagementBox) {
        if (Boxes.containsKey(engagementBox)) {
            Triple<Integer, Integer, Integer> t = Boxes.get(engagementBox);
            return new Pair<>(t.getLeft(), t.getMiddle());
        }
        return null;
    }

    public static int getWeddingRingForEngagementBox(int engagementBox) {
        if (Boxes.containsKey(engagementBox)) {
            return Boxes.get(engagementBox).getRight();
        }
        return -1;
    }

    private static void resetRelationship(Relationship rltn) {
        rltn.setMarriageId(0);
        rltn.setEngagementBoxId(0);
        rltn.setGroomId(0);
        rltn.setBrideId(0);
        rltn.setStatus(Relationship.Status.Single);
    }

    private static byte[] getEngagementResult(byte action) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_RESULT.getValue());
        mplew.write(action);
        return mplew.getPacket();
    }

    private static byte[] getEngagementSuccess(MapleCharacter groom, MapleCharacter bride) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_RESULT.getValue());
        Relationship rltn = groom.getRelationship();
        mplew.write(0xb);
        mplew.writeInt(0); // marriageId
        mplew.writeInt(groom.getId());
        mplew.writeInt(bride.getId());
        mplew.writeShort(1); // ?
        mplew.writeInt(Boxes.get(rltn.getEngagementBoxId()).getRight()); // wedding ring
        mplew.writeInt(Boxes.get(rltn.getEngagementBoxId()).getRight()); // wedding ring
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(groom.getName(), '\0', 13));
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(bride.getName(), '\0', 13));
        return mplew.getPacket();
    }

    /**
     * Get the engagement confirmation pop-up window
     *
     * @param username      the username of the player proposing
     * @param brideId       the player Id of the partner player
     * @param brideUsername the username of the partner player
     * @return a byte array containing the packet data
     */
    private static byte[] sendEngagementRequest(String username, int brideId, String brideUsername) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_REQUEST.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(username);
        mplew.writeInt(brideId); // not sure
        mplew.writeMapleAsciiString(brideUsername);
        return mplew.getPacket();
    }
}
