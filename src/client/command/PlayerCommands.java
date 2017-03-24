package client.command;

import client.MapleCharacter;
import client.MapleClient;
import net.server.channel.Channel;
import scripting.npc.NPCScriptManager;
import server.events.custom.Events;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * @author izarooni
 */
public class PlayerCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();
        Channel ch = client.getChannelServer();

        if (command.equals("help", "commands")) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add("@help - to see what commands there are");
            commands.add("@commands - another way to see the commands");
            commands.add("@rates - show the server rates");
            commands.add("@joinevent - join the event");
            commands.add("@leaveevent - leave the event");
            commands.add("@dispose - Dispose yourself (if you can't interact with npcs, etc) ");
            commands.add("@points = Everything about your points");
            commands.add("@achievements - Shows your current achievements");
            commands.add("@home - go to the home grounds");
            commands.add("@online - Show whoever is online");
            commands.add("@go <town|list> - warps you to a town or shows you a list of warpable towns");
            commands.add("@style - open the styling npc");
            commands.add("@kin - alternative to opening the syle npc");
            commands.add("@callgm <player> <reason> - report a player, insert your own name if it is for another reason.");
            commands.add("@report <bug> - report a bug, give as much detail as possible.");
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("rates")) {
            player.dropMessage(5, "EXP rate: " + player.getExpRate());
            player.dropMessage(5, "Drop rate: " + player.getDropRate());
            player.dropMessage(5, "Meso rate: " + player.getMesoRate());
        } else if (command.equals("joinevent", "leaveevent")) {
            Events events = Events.getInstance();
            if (command.equals("joinevent")) {
                events.joinEvent(player);
            } else {
                events.leaveEvent(player);
            }
        } else if (command.equals("points")) {
            player.dropMessage(6, "Fishing Points: " + player.getFishingPoints());
            player.dropMessage(6, "Vote Points: " + player.getClient().getVotePoints());
            player.dropMessage("Event points: " + player.getEventPoints());
            player.dropMessage(6, "Donation points: " + 0);
            player.dropMessage(6, "Shadow points: " + 0);
        } else if (command.equals("dispose")) {
            NPCScriptManager.getInstance().dispose(client);
            player.getClient().removeClickedNPC();
            player.announce(MaplePacketCreator.enableActions());
        } else if (command.equals("achievements")) {
            player.getClient().announce(MaplePacketCreator.getNPCTalk(9040004, (byte) 0, "These are the currently available achievements, blue means they are unlocked, red is locked. \r\n\r\n" + player.getAchievements().getAll(), "00 00", (byte) 3));
        } else if (command.equals("home")) {
            player.changeMap(240070101);
        } else if (command.equals("online")) {
            for (Channel channel : client.getWorldServer().getChannels()) {
                StringBuilder sb = new StringBuilder();
                for (MapleCharacter players : channel.getPlayerStorage().getAllCharacters()) {
                    if (!players.isGM()) {
                        sb.append(players.getName()).append(" ");
                    }
                }
                player.dropMessage(String.format("Channel(%d): %s", channel.getId(), sb.toString()));
            }
        } else if (command.equals("go")) {
            WeakHashMap<String, Integer> maps = new WeakHashMap<>();
            maps.put("fm", 910000000);
            maps.put("henesys", 100000000);
            maps.put("florina", 110000000);
            maps.put("nautilus", 120000000);
            maps.put("ereve", 130000000);
            maps.put("rien", 140000000);
            maps.put("orbis", 200000000);
            maps.put("ludi", 220000000);
            maps.put("aqua", 230000000);
            maps.put("leafre", 240000000);
            maps.put("mulung", 250000000);
            maps.put("ariant", 260000000);
            maps.put("timetemple", 270000000);
            maps.put("ellin", 300000000);
            maps.put("arcade", 970000000);
            if (args.length() == 1) {
                String name = args.get(0);
                if (maps.containsKey(name)) {
                    MapleMap map = ch.getMapFactory().getMap(maps.get(name));
                    if (map != null) {
                        player.changeMap(map);
                    } else {
                        player.dropMessage("An error occurred");
                    }
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String s : maps.keySet()) {
                sb.append(s).append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.dropMessage(String.format("'%s' is an invalid map. Try one of the following:", args.get(0)));
            player.dropMessage(sb.toString());
            maps.clear();
        } else if (command.equals("cleardrops")) {
            player.getMap().clearDrops();
        } else if (command.equals("save")) {
            player.saveToDB();
        } else if (command.equals("callgm")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                if (!message.isEmpty()) {
                    for (MapleCharacter players : client.getWorldServer().getPlayerStorage().getAllCharacters()) {
                        if (players.isGM()) {
                            players.dropMessage(String.format("[GM_CALL] %s : %s", player.getName(), message));
                        }
                    }
                    player.dropMessage("Help message sent");
                } else {
                    player.dropMessage("You must specify a message");
                }
            } else {
                player.dropMessage("You must specify a message");
            }
        } else if (command.equals("style", "styler", "stylist")) {
            NPCScriptManager.getInstance().start(player.getClient(), 9900000, player);
        } else if (command.equals("report")) {
            if (args.length() > 1) {
                String username = args.get(0);
                String message = args.concatFrom(1);
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    for (MapleCharacter players : client.getWorldServer().getPlayerStorage().getAllCharacters()) {
                        if (players.isGM()) {
                            players.dropMessage(String.format("[REPORT] %s : %s", username, message));
                        }
                    }
                } else {
                    player.dropMessage(String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage("You must specify a username and message");
            }
        }
    }
}
