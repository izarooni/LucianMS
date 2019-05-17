package com.lucianms.client;

import tools.MaplePacketCreator;

/**
 * Trying out something new here...
 *
 * @author izarooni
 */
public enum ChatType {

    NORMAL {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, show));
        }
    },

    WHITE {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, true, show));
        }
    }, // GM CHAT

    ORANGE {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 0));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // BUDDY

    PINK {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 1));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // PARTY

    PURPLE {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 2));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // GUILD

    LIGHT_GREEN {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 3));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // ALLIANCE

    RED {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // SERVER NOTICE

    BLUE {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // SERVER NOTICE

    GREEN_IN {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            int channel = player.getClient().getChannel();
            player.getMap().broadcastMessage(MaplePacketCreator.getWhisper(player.getName(), channel, message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }, // WHISPER IN

    YELLOW {
        @Override
        public void sendChat(MapleCharacter player, String message, boolean show) {
            player.getMap().broadcastMessage(MaplePacketCreator.sendYellowTip(player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, true));
        }
    }; // YELLOW TIPS

    public void sendChat(MapleCharacter player, String message, boolean show) {
        throw new AbstractMethodError();
    }
}
