package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.command.CommandWorker;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public final class PlayerWhisperEvent extends PacketEvent {

    private String username;
    private String content;
    private byte action;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        if (action == 6) {
            username = reader.readMapleAsciiString();
            content = reader.readMapleAsciiString();
        } else if (action == 5 || action == 0x44) {
            username = reader.readMapleAsciiString();
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (action == 6) { // whisper
            if (CommandWorker.isCommand(content)) {
                if (CommandWorker.process(getClient(), content, false)) {
                    return null;
                }
            }
            MapleCharacter target = getClient().getChannelServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
            if (target != null) {
                target.getClient().announce(MaplePacketCreator.getWhisper(getClient().getPlayer().getName(), getClient().getChannel(), content));

                if (target.isHidden() && target.gmLevel() > getClient().getPlayer().gmLevel()) {
                    getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 0));
                } else {
                    getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 1));
                }
            } else {// not found
                MapleWorld world = getClient().getWorldServer();
                if (world.isConnected(username)) {
                    world.whisper(getClient().getPlayer().getName(), username, getClient().getChannel(), content);

                    target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null && target.isHidden() && target.gmLevel() > getClient().getPlayer().gmLevel())
                        getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 0));
                    else
                        getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 1));
                } else {
                    getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 0));
                }
            }
        } else if (action == 5 || action == 0x44) { // - /find
            MapleCharacter target = getClient().getChannelServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
            if (action == 0x44 && target != null) {
                if (player.getBuddylist().containsVisible(target.getId()) && target.getBuddylist().containsVisible(player.getId())) {
                    // only find if they are mutual friends
                    getClient().announce(MaplePacketCreator.getBuddyFindResult(target, (byte) (target.getClient().getChannel() == getClient().getChannel() ? 1 : 3)));
                }
            } else if (target != null && getClient().getPlayer().gmLevel() >= target.gmLevel()) {
                if (target.getCashShop().isOpened()) {
                    getClient().announce(MaplePacketCreator.getFindReply(target.getName(), -1, 2));
                } else {
                    getClient().announce(MaplePacketCreator.getFindReply(target.getName(), target.getMap().getId(), 1));
                }
            } else { // not found
                byte channel = (byte) (getClient().getWorldServer().find(username) - 1);
                if (channel > -1) {
                    getClient().announce(MaplePacketCreator.getFindReply(username, channel, 3));
                } else {
                    getClient().announce(MaplePacketCreator.getWhisperReply(username, (byte) 0));
                }
            }
        }
        return null;
    }
}
