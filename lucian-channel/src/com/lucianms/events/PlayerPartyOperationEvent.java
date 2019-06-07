package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import com.lucianms.server.world.PartyOperation;
import tools.Functions;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerPartyOperationEvent extends PacketEvent {


    private String username;
    private byte action;
    private int partyID;
    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 3: // join
                partyID = reader.readInt();
                break;
            case 4:
                username = reader.readMapleAsciiString();
                break;
            case 5:
            case 6:
                playerID = reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleWorld world = getClient().getWorldServer();
        MapleParty party = player.getParty();

        switch (action) {
            case 1: { // create
                if (party != null) {
                    onLeaveOrDisband(player, party);
                }
                createParty();
                break;
            }
            case 2: { // leave or disband
                onLeaveOrDisband(player, party);
                break;
            }
            case 3: { // join
                if (player.getParty() == null) {
                    party = world.getParty(partyID);
                    if (party != null) {
                        if (party.size() < MapleParty.MaximumUsers) {
                            party.addMember(player);
                            player.receivePartyMemberHP();
                            player.updatePartyMemberHP();
                        } else {
                            getClient().announce(MaplePacketCreator.getPartyResult(17));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.getPartyResultMessage("The party you are trying to join no longer exists."));
                    }
                } else {
                    getClient().announce(MaplePacketCreator.getPartyResult(16));
                }
                break;
            }
            case 4: { // invite
                MapleCharacter invited = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                if (invited != null) {
                    if (invited.getPartyID() == 0) {
                        if (party == null) {
                            party = createParty();
                        }
                        if (party.size() < MapleParty.MaximumUsers) {
                            invited.getClient().announce(MaplePacketCreator.partyInvite(player));
                        } else {
                            getClient().announce(MaplePacketCreator.getPartyResult(17));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.getPartyResult(16));
                    }
                } else {
                    getClient().announce(MaplePacketCreator.getPartyResult(19));
                }
                break;
            }
            case 5: { // expel
                if (party != null) {
                    if (playerID == player.getId() || playerID == party.getLeaderPlayerID()) {
                        return null;
                    }
                    MaplePartyCharacter remove = party.remove(playerID);
                    if (remove != null) {
                        party.sendPacket(MaplePacketCreator.updateParty(remove.getChannelID(), party, PartyOperation.EXPEL, remove));
                    }
                }
                break;
            }
            case 6: { // change leader
                if (party != null && party.getLeaderPlayerID() == player.getId()) {
                    MaplePartyCharacter member = party.get(playerID);
                    if (member != null && playerID != player.getId()) {
                        party.setLeader(member.getPlayerID());
                    }
                }
                break;
            }
        }
        return null;
    }

    private void onLeaveOrDisband(MapleCharacter player, MapleParty party) {
        if (party != null) {
            MaplePartyCharacter member = party.get(player.getId());
            if (party.getLeaderPlayerID() == player.getId()) {
                Functions.requireNotNull(player.getEventInstance(), EventInstanceManager::disbandParty);
                party.sendPacket(MaplePacketCreator.updateParty(player.getClient().getChannel(), party, PartyOperation.DISBAND, member));
                party.dispose();
            } else {
                party.sendPacket(MaplePacketCreator.updateParty(player.getClient().getChannel(), party, PartyOperation.LEAVE, member));
                party.removeMember(player, false);
                Functions.requireNotNull(player.getEventInstance(), eim -> eim.leftParty(player));
            }
        }
    }

    private MapleParty createParty() {
        MapleCharacter player = getClient().getPlayer();
        MapleParty party = new MapleParty(player);
        MaplePartyCharacter member = party.get(player.getId());
        getClient().getWorldServer().getParties().put(party.getID(), party);
        getClient().announce(MaplePacketCreator.partyCreated(member));
        return party;
    }
}