package client.command;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import discord.Discord;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.World;
import server.events.custom.ManualPlayerEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import sx.blah.discord.util.MessageBuilder;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.ArrayList;
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

        if (command.equals("event")) {
            if (args.length() > 0) {
                switch (args.get(0)) {
                    case "new": {
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
                    }
                    case "cancel": {
                        if (playerEvent != null) {
                            world.setPlayerEvent(null);
                            playerEvent.garbage();
                            player.dropMessage("You have cancelled the event");
                        } else {
                            player.dropMessage("There is no event on this channel right now");
                        }
                        return true;
                    }
                }
                if (playerEvent != null) {
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
                            String eventName = (playerEvent.getName() == null) ? "event" : playerEvent.getName();
                            playerEvent.broadcastMessage(String.format("%s is hosting a(n) %s in channel %d, use @joinevent to join!", player.getName(), eventName, playerEvent.getChannel().getId()));

                            String message = String.format("%s is hosting a(n) %s in channel %d", player.getName(), eventName, playerEvent.getChannel().getId());
                            if (playerEvent.getGateTime() > 0) {
                                message += " and the gate will close in " + playerEvent.getGateTime() + " seconds";
                            }
                            new MessageBuilder(Discord.getBot().getClient()).withChannel(Long.parseLong("306540306302763030")).appendContent(message).build();
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
                            world.setPlayerEvent(null);
                            playerEvent.garbage();
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
                }
            } else {
                player.dropMessage("Incorrect command usage. Use < !event help > for help on configuring your event");
            }
            return true;
        } else if (command.equals("lock", "lockm")) {
            MobSkill skill = MobSkillFactory.getMobSkill(120, 1);
            if (command.equals("lockm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    if (!players.isGM()) {
                        player.giveDebuff(MapleDisease.SEAL, skill);
                    }
                }
            } else {
                if (args.length() > 0) {
                    for (int i = 0; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                        if (target != null && !target.isGM()) {
                            target.giveDebuff(MapleDisease.SEAL, skill);
                        }
                    }
                    player.dropMessage(6, "Done");
                } else {
                    player.dropMessage(5, "You must specify at least 1 username");
                }
            }
            return true;
        } else if (command.equals("reverse", "reversemap", "rev", "revm")) {
            boolean map = command.equals("reversemap", "revm");
            try {
                if (args.length() > 0) {
                    MobSkill skill = MobSkillFactory.getMobSkill(132, 2);
                    Long a1 = args.parseNumber(0);
                    if (a1 == null) {
                        player.dropMessage(5, args.getError(0));
                        return true;
                    }
                    if (map) {
                        for (MapleCharacter players : player.getMap().getCharacters()) {
                            if (!players.isGM()) {
                                players.giveDebuff(MapleDisease.CONFUSE, skill);
                            }
                        }
                    } else {
                        for (int i = 1; i < args.length(); i++) {
                            String username = args.get(i);
                            MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                            if (target != null && !target.isGM()) {
                                target.setChair(0);
                                target.announce(MaplePacketCreator.cancelChair(-1));
                                target.getMap().broadcastMessage(target, MaplePacketCreator.showChair(target.getId(), 0), false);
                                target.giveDebuff(MapleDisease.CONFUSE, skill);
                            }
                        }
                        player.dropMessage(6, "Done!");
                    }
                } else {
                    if (map) {
                        player.dropMessage(5, "Syntax: !" + command.getName() + " <mode>");
                    } else {
                        player.dropMessage(5, "Syntax: !" + command.getName() + " <mode> <usernames>");
                    }
                }
            } catch (NullPointerException e) {
                player.dropMessage(5, "An unknown error occurred, but we're too lazy to fix it. Fuck you!");
            }
            return true;
        } else if (command.equals("seduce", "sed", "seducem", "sedm")) {
            boolean map = command.equals("sedm", "seducem");
            if (args.length() > 0) {
                String direction = args.get(0);
                int level;
                if (direction.equalsIgnoreCase("right")) {
                    level = 2;
                } else if (direction.equalsIgnoreCase("down")) {
                    level = 7;
                } else if (direction.equalsIgnoreCase("left")) {
                    level = 1;
                } else {
                    player.dropMessage(5, "The only seduces available are: right, left and down");
                    return true;
                }
                MobSkill skill = MobSkillFactory.getMobSkill(128, level);
                if (!map) { // !<cmd_name> <direction> <usernames>
                    if (args.length() > 1) {
                        for (int i = 1; i < args.length(); i++) {
                            String username = args.get(i);
                            MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                            if (target != null) {
                                target.giveDebuff(MapleDisease.SEDUCE, skill);
                            }
                        }
                        player.dropMessage(6, "Done!");
                    } else {
                        player.dropMessage(5, "Syntax: !" + command.getName() + " <left/right/down> <usernames>");
                    }
                } else { // !<cmd_name> <direction>
                    for (MapleCharacter players : player.getMap().getCharacters()) {
                        players.giveDebuff(MapleDisease.SEDUCE, skill);
                    }
                }
            } else {
                player.dropMessage(5, "Syntax: !" + command.getName() + " <direction>" + (!map ? " <usernames>" : ""));
            }
            return true;
        } else if (command.equals("partycheck")) {
            ArrayList<Integer> parties = new ArrayList<>();
            ArrayList<String> solo = new ArrayList<>(); // character not in a party
            // get parties and solo players
            for (MapleCharacter players : player.getMap().getCharacters()) {
                if (players.getParty() != null) {
                    if (players.getParty().getLeader().getId() == players.getId() && !parties.contains(players.getPartyId())) {
                        parties.add(players.getPartyId());
                    }
                } else {
                    solo.add(players.getName());
                }
            }
            // get characters from each party
            for (int partyId : parties) {
                StringBuilder sb = new StringBuilder();
                MapleParty party = client.getWorldServer().getParty(partyId);
                sb.append(party.getLeader().getName()).append("'s members: ");
                for (MaplePartyCharacter members : party.getMembers()) {
                    if (members.getId() != party.getLeader().getId()) {
                        sb.append(members.getName()).append(", ");
                    }
                }
                if (party.getMembers().size() > 1) {
                    sb.setLength(sb.length() - 2);
                }
                player.dropMessage(sb.toString());
            }
            player.dropMessage("Characters NOT in party:");
            player.dropMessage(solo.toString());
            return true;
        } else if (command.equals("ak")) {
            if (args.length() == 1) {
                if (args.get(0).equalsIgnoreCase("reset")) {
                    player.getMap().setAutoKillPosition(null);
                    player.dropMessage(6, "Auto kill position removed");
                } else {
                    player.dropMessage(5, "Use < !ak reset > to remove auto kill");
                }
                return true;
            }
            player.getMap().setAutoKillPosition(player.getPosition());
            Point ak = player.getMap().getAutoKillPosition();
            player.dropMessage(6, String.format("Auto kill position set to: x: %d, y: %d", ak.x, ak.y));
        } else if (command.equals("bomb", "bombmap", "bombm")) {
            if (command.equals("bombmap", "bombm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
                    if (bomb == null) {
                        player.dropMessage(5, "An error occured");
                        return true;
                    }
                    for (int i = -5; i < 5; i++) {
                        Point pos = players.getPosition().getLocation();
                        pos.x += (i * 30);
                        player.getMap().spawnMonsterOnGroudBelow(bomb, pos);
                    }
                }
            } else {
                MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
                if (bomb == null) {
                    player.dropMessage(5, "An error occured");
                    return true;
                }
                int time = 2;
                MapleCharacter target;
                if (args.length() > 0) {
                    int i1;
                    String username = args.get(0);
                    target = ch.getPlayerStorage().getCharacterByName(username);
                    if (target != null) {
                        target.getMap().spawnMonsterOnGroudBelow(bomb, player.getPosition());
                    } else {
                        player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                        return true;
                    }
                    if ((i1 = args.findArg("-t")) > -1) {
                        Long a1 = args.parseNumber(i1);
                        if (a1 == null) {
                            player.dropMessage(5, args.getError(i1));
                            return true;
                        }
                        time = a1.intValue();
                    }
                } else {
                    target = player;
                }
                if (time < 0) {
                    time = 0;
                }
                bomb.getStats().getSelfDestruction().setRemoveAfter(time * 1000);
                target.getMap().spawnMonsterOnGroudBelow(bomb, target.getPosition());
            }
            return true;
        }
        return false;
    }
}
