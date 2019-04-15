package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.world.MapleMessenger;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerMessengerEvent extends PacketEvent {

    private String content;
    private String username;
    private byte action;
    private int messengerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 0:
                messengerID = reader.readInt();
                break;
            case 3:
                content = reader.readMapleAsciiString();
                break;
            case 5:
                username = reader.readMapleAsciiString();
                break;
            case 6:
                content = reader.readMapleAsciiString();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleWorld world = getClient().getWorldServer();
        MapleMessenger messenger = player.getMessenger();

        switch (action) {
            case 0: // create
                if (messenger == null) {
                    if (messengerID == 0) {
                        messenger = new MapleMessenger(player);
                        world.getMessengers().put(messenger.getID(), messenger);
                    } else {
                        messenger = world.getMessenger(messengerID);
                        if (messenger != null) {
                            messenger.addMember(player);
                        }
                    }
                }
                break;
            case 2: // leave
                if (messenger != null) {
                    messenger.removeMember(player);
                }
                break;
            case 3: // invite
                if (messenger.size() == MapleMessenger.MaximumUsers) {
                    MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(content));
                    if (target != null) {
                        if (target.getMessenger() == null) {
                            target.getClient().announce(MaplePacketCreator.messengerInvite(player.getName(), messenger.getID()));
                            getClient().announce(MaplePacketCreator.messengerNote(content, 4, 1));
                        } else {
                            getClient().announce(MaplePacketCreator.messengerChat(player.getName() + " : " + content + " is already using Maple Messenger"));
                        }
                    } else {
                        getClient().announce(MaplePacketCreator.messengerNote(content, 4, 0));
                    }
                } else {
                    getClient().announce(MaplePacketCreator.messengerChat(player.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
                }
                break;
            case 5:
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    if (target.getMessenger() != null) {
                        target.getClient().announce(MaplePacketCreator.messengerNote(player.getName(), 5, 0));
                    }
                }
                break;
            case 6: // chat
                if (messenger != null) {
                    messenger.sendPacket(MaplePacketCreator.messengerChat(String.format("%s : %s", player.getName(), content)));
                }
                break;
        }
        return null;
    }
}
