package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.Relationship;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author izarooni
 */
public class PlayerRingActionEvent extends PacketEvent {

    private static final HashMap<Integer, Triple<Integer, Integer, Integer>> Boxes = new HashMap<>();

    static {
        // format: engagement box, Pair(empty engagement box, engagement ring, wedding ring)
        Boxes.put(2240000, new Triple<>(4031357, 4031358, 1112803)); // moonstone
        Boxes.put(2240001, new Triple<>(4031359, 4031370, 1112806)); // star gem
        Boxes.put(2240002, new Triple<>(4031361, 4031362, 1112807)); // golden heart
        Boxes.put(2240003, new Triple<>(4031363, 4031364, 1112809)); // silver swan
    }

    private String username;
    private byte action;
    private int itemID;
    private int playerID;
    private short slot;
    private boolean accepted;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 0:
                username = reader.readMapleAsciiString();
                itemID = reader.readInt();
                break;
            case 2:
                accepted = reader.readByte() == 1;
                username = reader.readMapleAsciiString();
                playerID = reader.readInt();
                break;
            case 3:
                itemID = reader.readInt();
                break;
            case 6:
                slot = reader.readShort();
                reader.skip(2);
                itemID = reader.readInt();
                break;
            case 9:
//                byte count = reader.readByte();
//                for (int i = 0; i < count; i++) {
//                    String entry = reader.readMapleAsciiString();
//                }
                break;
            default: {
                getLogger().warn("unhandled action {}\r\n{}", action, reader.toString());
                break;
            }
        }
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleWorld world = client.getWorldServer();
        MapleChannel ch = client.getChannelServer();
        MapleCharacter player = client.getPlayer();
        Relationship rltn = player.getRelationship();

        MapleCharacter pplayer = null; // partner player
        Relationship prltn = null; // partner relationship
        int partnerId = (rltn.getGroomId() == player.getId()) ? rltn.getBrideId() : rltn.getGroomId();
        if (partnerId > 0) {
            pplayer = world.getPlayerStorage().get(partnerId);
            if (pplayer != null) {
                prltn = pplayer.getRelationship();
            }
        }

        switch (action) {
            case 0: { // proposal
                if (username.equalsIgnoreCase(player.getName())) {
                    client.announce(getEngagementResult((byte) 0x12));
                } else if (rltn.getStatus() == Relationship.Status.Single && rltn.getBrideId() > 0) {
                    client.announce(getEngagementResult((byte) 0x1b));
                } else {
                    if (!Boxes.containsKey(itemID)) {
                        getLogger().warn("'{}' attempting to propose with a non-engagement box item ({})", player.getName(), itemID);
                        return null;
                    } else if (player.getInventory(MapleInventoryType.USE).findById(itemID) == null) {
                        getLogger().warn("'{}' attempting to propose with an invalid item ({})", player.getName(), itemID);
                        return null;
                    }
                    pplayer = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (pplayer != null && pplayer.getClient().getChannel() == ch.getId()) {
                        prltn = pplayer.getRelationship();
                        if (pplayer.getMapId() != player.getMapId()) {
                            // Both players must be in the same map
                            client.announce(getEngagementResult((byte) 0x13));
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
                            rltn.setEngagementBoxId(itemID);
                            prltn.setEngagementBoxId(itemID);

                            prltn.setBrideId(pplayer.getId());
                            prltn.setGroomId(player.getId());
                            rltn.setBrideId(pplayer.getId());
                            rltn.setGroomId(player.getId());

                            pplayer.announce(sendEngagementRequest(player.getName(), player.getId(), username));
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
                    return null;
                }
                rltn.reset();
                if (pplayer != null) {
                    pplayer.announce(getEngagementResult((byte) 0x1d));
                    prltn.reset();
                }
                break;
            }
            case 2: { // request: result
                if (rltn.getStatus() != Relationship.Status.Single || rltn.getGroomId() == 0 || rltn.getBrideId() == 0) {
                    return null;
                }
                if (playerID != rltn.getGroomId()) {
                    // attempting to engage with someone else
                    getLogger().warn("'{}' attempting to become engaged with somebody else ('{}': {}) other than proposed person ('{}')", player.getName(), username, playerID, MapleCharacter.getNameById(rltn.getBrideId()));
                    return null;
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
                            player.announce(getEngagementSuccess(pplayer, player));
                            pplayer.announce(getEngagementSuccess(pplayer, player));
                            rltn.setStatus(Relationship.Status.Engaged);
                            prltn.setStatus(Relationship.Status.Engaged);
                            MapleInventoryManipulator.removeById(pplayer.getClient(), MapleInventoryType.USE, pplayer.getRelationship().getEngagementBoxId(), 1, false, false);
                            // male gets box and female gets ring :D
                            MapleInventoryManipulator.addById(pplayer.getClient(), p.getLeft(), (short) 1); // (empty) engagement box
                            MapleInventoryManipulator.addById(client, p.getRight(), (short) 1); // engagement ring

                            try (Connection con = player.getClient().getWorldServer().getConnection()) {
                                rltn.save(con);
                            } catch (SQLException e) {
                                getLogger().warn("Unable to save relationship data for player '{}': {}", player.getName(), e.getMessage());
                            }
                        }
                    } else {
                        pplayer.announce(getEngagementResult((byte) 0x1e));

                        rltn.reset();
                        prltn.reset();
                    }
                }
                break;
            }
            case 3: { // drop ring / engagement box
                if (player.getInventory(MapleInventoryType.ETC).findById(itemID) == null) {
                    getLogger().warn("Attempting to drop null item ({})", itemID);
                    return null;
                }
                Triple<Integer, Integer, Integer> box = Boxes.get(rltn.getEngagementBoxId());
                if (box == null || itemID != box.getLeft() && itemID != box.getMiddle()) {
                    getLogger().warn("'{}' dropped an invalid ring/box ({})", player.getName(), itemID);
                    return null;
                }
                client.announce(getEngagementResult((byte) 0xd));
                MapleInventoryManipulator.removeById(client, MapleInventoryType.ETC, itemID, 1, false, false);
                rltn.reset();

                if (pplayer != null) {
                    pplayer.announce(getEngagementResult((byte) 0xd)); // am i supposed to send this to both players? hmmm
                    itemID = (itemID == box.getLeft() ? box.getRight() : box.getLeft());
                    MapleInventoryManipulator.removeById(pplayer.getClient(), MapleInventoryType.ETC, itemID, 1, false, false);
                    prltn.reset();
                }
                break;
            }
            case 6: { // wedding invitation
                Item item = player.getInventory(MapleInventoryType.ETC).getItem(slot);
                if (item != null && item.getItemId() == itemID) {

                }
                break;
            }
            case 9: { // groom's wish list
                client.announce(MaplePacketCreator.sendGroomWishlist());
                break;
            }
        }
        return null;
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

    private static byte[] getEngagementResult(byte action) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MARRIAGE_RESULT.getValue());
        w.write(action);
        return w.getPacket();
    }

    public static byte[] getEngagementSuccess(MapleCharacter groom, MapleCharacter bride) {
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MARRIAGE_RESULT.getValue());
        Relationship rltn = groom.getRelationship();
        w.write(0xb);
        w.writeInt(0); // marriageId
        w.writeInt(groom.getId());
        w.writeInt(bride.getId());
        w.writeShort(1); // ?
        w.writeInt(Boxes.get(rltn.getEngagementBoxId()).getRight()); // wedding ring
        w.writeInt(Boxes.get(rltn.getEngagementBoxId()).getRight()); // wedding ring
        w.writeAsciiString(StringUtil.getRightPaddedStr(groom.getName(), '\0', 13));
        w.writeAsciiString(StringUtil.getRightPaddedStr(bride.getName(), '\0', 13));
        return w.getPacket();
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
        MaplePacketWriter w = new MaplePacketWriter();
        w.writeShort(SendOpcode.MARRIAGE_REQUEST.getValue());
        w.write(0);
        w.writeMapleString(username);
        w.writeInt(brideId); // not sure
        w.writeMapleString(brideUsername);
        return w.getPacket();
    }
}
