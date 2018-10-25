package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleMessenger;
import com.lucianms.server.world.MapleMessengerCharacter;
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
        MapleChannel channel = getClient().getChannelServer();
        MapleMessenger messenger = player.getMessenger();

        switch (action) {
            case 0x00:
                if (messenger == null) {
                    if (messengerID == 0) {
                        MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, 0);
                        messenger = world.createMessenger(messengerplayer);
                        player.setMessenger(messenger);
                        player.setMessengerPosition(0);
                    } else {
                        messenger = world.getMessenger(messengerID);
                        int position = messenger.getLowestPosition();
                        MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, position);
                        if (messenger.getMembers().size() < 3) {
                            player.setMessenger(messenger);
                            player.setMessengerPosition(position);
                            world.joinMessenger(messenger.getId(), messengerplayer, player.getName(), messengerplayer.getChannel());
                        }
                    }
                }
                break;
            case 0x02:
                if (messenger != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, player.getMessengerPosition());
                    world.leaveMessenger(messenger.getId(), messengerplayer);
                    player.setMessenger(null);
                    player.setMessengerPosition(4);
                }
                break;
            case 0x03:
                if (messenger.getMembers().size() < 3) {
                    MapleCharacter target = channel.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(content));
                    if (target != null) {
                        if (target.getMessenger() == null) {
                            target.getClient().announce(MaplePacketCreator.messengerInvite(player.getName(), messenger.getId()));
                            getClient().announce(MaplePacketCreator.messengerNote(content, 4, 1));
                        } else {
                            getClient().announce(MaplePacketCreator.messengerChat(player.getName() + " : " + content + " is already using Maple Messenger"));
                        }
                    } else {
                        if (world.find(content) > -1) {
                            world.messengerInvite(getClient().getPlayer().getName(), messenger.getId(), content, getClient().getChannel());
                        } else {
                            getClient().announce(MaplePacketCreator.messengerNote(content, 4, 0));
                        }
                    }
                } else {
                    getClient().announce(MaplePacketCreator.messengerChat(player.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
                }
                break;
            case 0x05:
                MapleCharacter target = channel.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    if (target.getMessenger() != null) {
                        target.getClient().announce(MaplePacketCreator.messengerNote(player.getName(), 5, 0));
                    }
                } else {
                    world.declineChat(username, player.getName());
                }
                break;
            case 0x06:
                if (messenger != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, player.getMessengerPosition());
                    world.messengerChat(messenger, content, messengerplayer.getName());
                }
                break;
        }
        return null;
    }
}
