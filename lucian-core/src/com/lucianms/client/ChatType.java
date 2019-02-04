package com.lucianms.client;

import tools.MaplePacketCreator;

/**
 * Trying out something new here...
 *
 * @author izarooni
 */
public enum ChatType {

    NORMAL {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, show));
        }
    },

    WHITE {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, true, show));
        }
    }, // GM CHAT

    ORANGE {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 0));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // BUDDY

    PINK {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 1));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // PARTY

    PURPLE {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 2));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // GUILD

    LIGHT_GREEN {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.multiChat(player.getName(), message, 3));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // ALLIANCE

    RED {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // SERVER NOTICE

    BLUE {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // SERVER NOTICE

    GREEN_IN {
        public void sendChat(MapleCharacter player, String message, int show) {
            int channel = player.getClient().getChannel();
            player.getMap().broadcastMessage(MaplePacketCreator.getWhisper(player.getName(), channel, message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }, // WHISPER IN

    YELLOW {
        public void sendChat(MapleCharacter player, String message, int show) {
            player.getMap().broadcastMessage(MaplePacketCreator.sendYellowTip(player.getName() + " : " + message));
            player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
        }
    }; // YELLOW TIPS

    public void sendChat(MapleCharacter player, String message, int show) {
        throw new AbstractMethodError();
    }
}
