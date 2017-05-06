package client.command;

import client.MapleCharacter;
import client.MapleClient;
import net.server.channel.Channel;
import net.server.world.World;
import server.events.custom.ManualPlayerEvent;

import java.util.Map;

/**
 * This can get messy so I'm separating event related commands from regular GM commands
 *
 * @author izarooni
 */
public class EventCommands {

    public static boolean execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        final MapleCharacter player = client.getPlayer();
        final Channel ch = client.getChannelServer();
        final World world = client.getWorldServer();
        ManualPlayerEvent playerEvent = client.getWorldServer().getPlayerEvent();

        if (command.equals("startevent")) {
            if (playerEvent == null) {
                world.setPlayerEvent((playerEvent = new ManualPlayerEvent(player)));
                playerEvent.setMap(player.getMap());
                playerEvent.setChannel(ch);
                player.dropMessage("Event creation started. To get help configuring use < !event help >");
                player.dropMessage("If you would rather immediately start the event with default values, use < !event start >");
            } else {
                player.dropMessage(5, "An event is already being hosted in this channel!");
                player.dropMessage(5, "Use < !event info > for more information");
            }
            return true;
        } else if (command.equals("cancelevent")) {
            if (playerEvent != null) {
                playerEvent.dispose();
                world.setPlayerEvent(null);
                player.dropMessage("You have cancelled the event");
            } else {
                player.dropMessage("There is no event on this channel right now");
            }
            return true;
        } else if (command.equals("event")) {
            if (playerEvent != null) {
                if (args.length() > 0) {
                    String action = args.get(0).toLowerCase();
                    switch (action) {
                        case "info": {
                            player.dropMessage("------------------------------");
                            player.dropMessage("Event host: " + playerEvent.getHost().getName());
                            player.dropMessage("Event name: " + playerEvent.getName());
                            player.dropMessage("Event map: " + playerEvent.getMap());
                            player.dropMessage("Event active: " + playerEvent.isOpen());
                            break;
                        }
                        case "help": {
                            player.dropMessage("!event name <name> - Set the name of the event");
                            player.dropMessage("!event sp - Set the spawn point of the event");
                            player.dropMessage("!event gate <time (seconds)> - Set the delay before the gate automatically closes");
                            player.dropMessage("!event cancel - Reset the event. Mainly used if you decide to not host an event");
                            player.dropMessage("!event winners <add/remove> <usernames> - Add or remove winners from the list of winners");
                            player.dropMessage("!event winners view - View all current winners and their points");
                            break;
                        }
                        case "start": {
                            playerEvent.openGates(playerEvent.getGateTime(), 90, 75, 60, 30, 15, 5, 3, 2, 1);
                            String name = (playerEvent.getName() == null) ? "event" : player.getName();
                            playerEvent.broadcastMessage(String.format("%s is hosting a(n) %s in channel %d, use @joinevent to join!", player.getName(), name, playerEvent.getChannel().getId()));
                            break;
                        }
                        case "name": {
                            if (args.length() > 1) {
                                String name = args.concatFrom(1);
                                playerEvent.setName(name);
                                player.dropMessage("Event name changed to " + name);
                            } else {
                                player.dropMessage("Incorrect command usage. Syntax: !event name <name>");
                            }
                            break;
                        }
                        case "spawnpoint": {
                            playerEvent.setSpawnPoint(player.getPosition());
                            player.dropMessage("Spawn point has been set to your position");
                            break;
                        }
                        case "gate": {
                            Long a1 = args.parseNumber(1);
                            if (a1 == null || args.getError(1) != null) {
                                player.dropMessage(args.getError(1));
                                return true;
                            }
                            int time = a1.intValue();
                            playerEvent.setGateTime(time);
                            player.dropMessage(String.format("Event time is now set to %d seconds", time));
                            break;
                        }
                        case "close": {
                            if (playerEvent.isOpen()) {
                                playerEvent.setOpen(false);
                                if (playerEvent.getGateTime() == 0) {
                                    // manual gate closing
                                    playerEvent.broadcastMessage("The gates are now closed");
                                } else {
                                    player.dropMessage("You have closed the event gates");
                                }
                            } else {
                                player.dropMessage("The event gates are already closed");
                            }
                            break;
                        }
                        case "end": {
                            if (!playerEvent.getWinners().isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Congrats to ");
                                for (Map.Entry<String, Integer> entry : playerEvent.getWinners().entrySet()) {
                                    sb.append(entry.getKey()).append("(").append(entry.getValue()).append(")").append(", ");
                                }
                                sb.setLength(sb.length() - 2);
                                String name = (playerEvent.getName() == null) ? "the event" : playerEvent.getName();
                                sb.append(" on winning ").append(name);
                                playerEvent.broadcastMessage(sb.toString());
                            }
                            playerEvent.dispose();
                            world.setPlayerEvent(null);
                            player.dropMessage("Event ended");
                            break;
                        }
                        case "winners": {
                            if (args.length() > 1) { // !event winners add/remove <usernames>
                                switch (args.get(1).toLowerCase()) {
                                    case "add": {
                                        if (args.length() > 2) {
                                            String[] usernames = args.concatFrom(2).split(" ");
                                            playerEvent.addWinners(usernames);
                                            if (usernames.length == 1) {
                                                player.dropMessage(usernames[0] + " added to list of winners");
                                            } else {
                                                player.dropMessage("Specified players are now winners");
                                            }
                                        } else {
                                            player.dropMessage("You must specify at least 1 username");
                                        }
                                        break;
                                    }
                                    case "remove": {
                                        if (args.length() > 2) {
                                            String[] usernames = args.concatFrom(2).split(" ");
                                            playerEvent.removeWinners(usernames);
                                            if (usernames.length == 1) {
                                                player.dropMessage(usernames[0] + " removed from list of winners");
                                            } else {
                                                player.dropMessage("Specified players are now longer winners");
                                            }
                                        } else {
                                            player.dropMessage("You must specify at least 1 username");
                                        }
                                        break;
                                    }
                                    case "view": {
                                        Map<String, Integer> w = playerEvent.getWinners();
                                        if (w.isEmpty()) {
                                            player.dropMessage("There are no winners right now");
                                        } else {
                                            player.dropMessage("Here are the current winners");
                                            StringBuilder sb = new StringBuilder();
                                            for (Map.Entry<String, Integer> entry : w.entrySet()) {
                                                sb.append(entry.getKey()).append("(").append(entry.getValue()).append(")").append(", ");
                                            }
                                            sb.setLength(sb.length() - 2);
                                            player.dropMessage(sb.toString());
                                        }
                                        break;
                                    }
                                    default:
                                        player.dropMessage("? what are you trying to do ?");
                                        return true;
                                }
                                player.dropMessage("There are now " + playerEvent.getWinners().size() + " in the winner list");
                            } else {
                                player.dropMessage("Incorrect command usage");
                            }
                            break;
                        }
                    }
                } else {
                    player.dropMessage("Incorrect command usage. Use < !event help > for help on configuring your event");
                }
            } else {
                player.dropMessage("There is no event on this channel right now");
            }
            return true;
        }
        return false;
    }
}
