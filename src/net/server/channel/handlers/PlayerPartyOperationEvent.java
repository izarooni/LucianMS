package net.server.channel.handlers;

import client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.MapleWorld;
import net.server.world.PartyOperation;
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
        MaplePartyCharacter partyplayer = player.getMPC();

        switch (action) {
            case 1: { // create
                if (player.getLevel() < 10) {
                    getClient().announce(MaplePacketCreator.partyStatusMessage(10));
                    return null;
                }
                if (player.getParty() == null) {
                    partyplayer = new MaplePartyCharacter(player);
                    party = world.createParty(partyplayer);
                    player.setParty(party);
                    player.setMPC(partyplayer);
                    player.silentPartyUpdate();
                    getClient().announce(MaplePacketCreator.partyCreated(partyplayer));
                } else {
                    getClient().announce(MaplePacketCreator.serverNotice(5, "You can't create a party as you are already in one."));
                }
                break;
            }
            case 2: {
                if (party != null && partyplayer != null) {
                    if (partyplayer.equals(party.getLeader())) {
                        world.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                        if (player.getEventInstance() != null) {
                            player.getEventInstance().disbandParty();
                        }
                    } else {
                        world.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                        if (player.getEventInstance() != null) {
                            player.getEventInstance().leftParty(player);
                        }
                    }
                    player.setParty(null);
                }
                break;
            }
            case 3: { // join
                if (player.getParty() == null) {
                    party = world.getParty(partyID);
                    if (party != null) {
                        if (party.getMembers().size() < 6) {
                            partyplayer = new MaplePartyCharacter(player);
                            world.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            player.receivePartyMemberHP();
                            player.updatePartyMemberHP();
                        } else {
                            getClient().announce(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.serverNotice(5, "The person you have invited to the party is already in one."));
                    }
                } else {
                    getClient().announce(MaplePacketCreator.serverNotice(5, "You can't join the party as you are already in one."));
                }
                break;
            }
            case 4: { // invite
                MapleCharacter invited = world.getPlayerStorage().getPlayerByName(username);
                if (invited != null) {
                    if (invited.getLevel() < 10) { //min requirement is level 10
                        getClient().announce(MaplePacketCreator.serverNotice(5, "The player you have invited does not meet the requirements."));
                        return null;
                    }
                    if (invited.getParty() == null) {
                        if (player.getParty() == null) {
                            partyplayer = new MaplePartyCharacter(player);
                            party = world.createParty(partyplayer);
                            player.setParty(party);
                            player.setMPC(partyplayer);
                            getClient().announce(MaplePacketCreator.partyCreated(partyplayer));
                        }
                        if (party.getMembers().size() < 6) {
                            invited.getClient().announce(MaplePacketCreator.partyInvite(player));
                        } else {
                            getClient().announce(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.partyStatusMessage(16));
                    }
                } else {
                    getClient().announce(MaplePacketCreator.partyStatusMessage(19));
                }
                break;
            }
            case 5: { // expel
                if (partyplayer.equals(party.getLeader())) {
                    MaplePartyCharacter expelled = party.getMemberById(playerID);
                    if (expelled != null) {
                        world.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                        if (player.getEventInstance() != null) {
                            if (expelled.isOnline()) {
                                player.getEventInstance().disbandParty();
                            }
                        }
                    }
                }
                break;
            }
            case 6: {
                if (party != null && party.getLeader().getId() == player.getId()) {
                    MaplePartyCharacter newLeadr = party.getMemberById(playerID);
                    party.setLeader(newLeadr);
                    world.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
                }
                break;
            }
        }
        return null;
    }
}