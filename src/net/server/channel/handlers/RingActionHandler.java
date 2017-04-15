package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.Relationship;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import net.SendOpcode;
import net.server.channel.Channel;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 */
public class RingActionHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        Channel ch = client.getChannelServer();
        Relationship rltn = player.getRelationship();

        byte action = slea.readByte();
        switch (action) {
            case 0: { // proposal
                String tUsername = slea.readMapleAsciiString();
                int itemId = slea.readInt();
                if (tUsername.equalsIgnoreCase(player.getName())) {
                    client.announce(getEngagementResult((byte) 0x12));
                } else if (rltn.getStatus() == Relationship.Status.Single && rltn.getPartnerName() != null) {
                    client.announce(getEngagementResult((byte) 0x1b));
                } else {
                    if (itemId < 2240000 || itemId > 2240003) {
                        // engagement boxes only
                        return;
                    } else if (player.getInventory(MapleInventoryType.USE).findById(itemId) == null) {
                        return;
                    }
                    MapleCharacter partner = ch.getPlayerStorage().getCharacterByName(tUsername);
                    if (partner != null) {
                        if (partner.getMapId() != player.getMapId()) {
                            client.announce(getEngagementResult((byte) 0x13));
                        } else if (partner.getGender() == player.getGender()) {
                            client.announce(getEngagementResult((byte) 0x16));
                        } else if (partner.getRelationship().getStatus() == Relationship.Status.Married) {
                            client.announce(getEngagementResult((byte) 0x18));
                        } else if (rltn.getStatus() == Relationship.Status.Married) {
                            client.announce(getEngagementResult((byte) 0x1a));
                        } else if (partner.getRelationship().getStatus() == Relationship.Status.Engaged) {
                            client.announce(getEngagementResult((byte) 0x19));
                        } else if (rltn.getStatus() == Relationship.Status.Engaged) {
                            client.announce(getEngagementResult((byte) 0x17));
                        } else if (partner.getRelationship().getStatus() == Relationship.Status.Single && partner.getRelationship().getPartnerName() != null) {
                            client.announce(getEngagementResult((byte) 0x1c));
                        } else {
                            // holy finally
                            partner.announce(sendEngagementRequest(player.getName(), player.getId(), tUsername));
                            partner.getRelationship().setPartnerName(player.getName());
                            rltn.setPartnerName(partner.getName());
                        }
                    } else {
                        client.announce(getEngagementResult((byte) 0x12));
                    }
                }
                break;
            }
            case 1: { // request: canceled
                if (rltn.getPartnerName() == null) {
                    // can't cancel a proposal if there isn't a proposal :thinking:
                    return;
                }
                MapleCharacter partner = ch.getPlayerStorage().getCharacterByName(rltn.getPartnerName());
                rltn.setPartnerName(null);
                if (partner != null) {
                    partner.getRelationship().setPartnerName(null);
                    partner.announce(getEngagementResult((byte) 0x1d));
                }
                break;
            }
            case 2: { // request: result
                boolean accepted = slea.readByte() == 1;
                String tUsername = slea.readMapleAsciiString();
                int partnerId = slea.readInt();
                if (rltn.getStatus() != Relationship.Status.Single || rltn.getPartnerName() == null) {
                    return;
                }
                if (!tUsername.equalsIgnoreCase(rltn.getPartnerName())) {
                    // attempting to engage with someone other than the requester
                    return;
                }
                MapleCharacter partner = ch.getPlayerStorage().getCharacterByName(tUsername);
                if (partner != null && partner.getRelationship().getStatus() == Relationship.Status.Single && partner.getRelationship().getPartnerName().equalsIgnoreCase(rltn.getPartnerName())) {
                    if (accepted) {
                        partner.announce(getEngagementResult((byte) 0xb));
                        rltn.setStatus(Relationship.Status.Engaged);
                        partner.getRelationship().setStatus(Relationship.Status.Engaged);
                    } else {
                        partner.announce(getEngagementResult((byte) 0x1e));
                        partner.getRelationship().setPartnerName(null);
                        rltn.setPartnerName(null);
                    }
                }
                break;
            }
            case 3: { // drop ring / engagement box
                int itemId = slea.readInt();
                /*
                "Your engagement has been broken." - A29B89
                case 0x0D:
                    v12 = *(_DWORD *)(sub_425D0B(&v47) + 4) + 1359;
                    LOBYTE(v78) = 5;
                    sub_474AB9(v12);
                    LOBYTE(v78) = 0;
                    sub_4289B7(&v47);
                    StringPool::GetInstance();
                    v13 = StringPool::GetString(&v71, 4238);
                    LOBYTE(v78) = 6;
                    sub_4181C9(v13);
                    v8 = v71;
                    LOBYTE(v78) = 0;
                 */
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
                System.out.println("Unhandled ring action " + slea.toString());
                break;
            }
        }
    }

    private static byte[] getEngagementResult(byte action) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_RESULT.getValue());
        mplew.write(action);
        if (action == 0xB) {
            mplew.write(new byte[0x30]);
        }
        return mplew.getPacket();
    }

    /**
     * Get the engagement confirmation pop-up window
     *
     * @param username  the username of the player proposing
     * @param partnerId the player Id of the partner player
     * @param partner   the username of the partner player
     * @return a byte array containing the packet data
     */
    private static byte[] sendEngagementRequest(String username, int partnerId, String partner) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MARRIAGE_REQUEST.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(username);
        mplew.writeInt(partnerId); // not sure
        mplew.writeMapleAsciiString(partner);
        return mplew.getPacket();
    }
}
