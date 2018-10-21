package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleDisease;
import com.lucianms.command.CommandWorker;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.life.MobSkillFactory;
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
        final MapleChannel ch = client.getChannelServer();
        final MapleWorld world = client.getWorldServer();
        ManualPlayerEvent playerEvent = client.getWorldServer().getPlayerEvent();

        if (command.equals("eventcommands")) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add("!event - setup/start/close an event");
            commands.add("!lock<m> - Disable skills for specified players or everybody (map)");
            commands.add("!reverse<m> - Give specified players or everybody the reverse debuff");
            commands.add("!seduce<m> - Give specified players or everybody the seduce debuff");
            commands.add("!partycheck - Check all players party information");
            commands.add("!ak - Set the auto kill position");
            commands.add("!bomb<m> - Spawn a timed bomb at your position or around everybody in the map");
            commands.sort(String::compareTo);
            commands.forEach(player::dropMessage);
        } else if (command.equals("help")) {
            // the staff help command must be declared in the lowest tier GM command system
            player.dropMessage("!eventcommands - Display the list of event commands");
            player.dropMessage("!gmcommands - Display the list of commands available to Game Masters and above");
            player.dropMessage("!hgmcommands - Display the list of commands available to Head GMs and above");
            player.dropMessage("!admincommands - Display the list of commands available to Administrators");
        } else if (command.equals("event")) {
            if (args.length() > 0) {
                switch (args.get(0)) {
                    case "new": {
                        if (playerEvent == null) {
                            world.setPlayerEvent((playerEvent = new ManualPlayerEvent(player)));
                            playerEvent.setMap(player.getMap());
                            playerEvent.setChannel(ch);
                            player.dropMessage("Event creation started. To set the name of your event, use: '!event name <event_name>'");
                            player.dropMessage("If you would rather immediately start the event with default values, use: '!event start'");
                            player.dropMessage("You may also abort this event creation via '!event cancel'");
                        } else {
                            player.dropMessage("An event is already being hosted in this channel!");
                            player.dropMessage("Use < !event info > for more information");
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
                    case "help": {
                        ArrayList<String> list = new ArrayList<>(8);
                        list.add("!event info - View configuration of the current event");
                        list.add("!event start - Open your event publicly");
                        list.add("!event cancel - Cancel configurations and closes the gates");

                        list.add("!event [name] - View or change the name of the current event");
                        list.add("!event sp - Set the spawn point of the event");
                        list.add("!event gate <time> - Set the delay (in seconds) before the gate automatically closes");
                        list.add("!event winners <add/remove> <usernames...> - Add or remove winners from the list of winners");
                        list.add("!event winners view - View all current winners and their points");
                        list.sort(String::compareTo);
                        list.forEach(player::sendMessage);
                        return true;
                    }
                }
                if (playerEvent != null) {
                    String action = args.get(0).toLowerCase();
                    switch (action) {
                        case "info": {
                            player.dropMessage("------------------------------");
                            player.dropMessage("Host: " + playerEvent.getHost().getName());
                            player.dropMessage("Name: " + playerEvent.getName());
                            player.sendMessage(6, "Map: <{}> {}", player.getMapId(), player.getMap().getMapName());
                            player.dropMessage("Gates: " + (playerEvent.isOpen() ? "open" : "closed"));
                            player.dropMessage("Gate delay: " + playerEvent.getGateTime());
                            player.dropMessage("Winners: " + playerEvent.getWinners().keySet());
                            break;
                        }
                        case "start": {
                            playerEvent.openGates(playerEvent.getGateTime(), 90, 75, 60, 30, 15, 5, 3, 2, 1);
                            String eventName = (playerEvent.getName() == null) ? "event" : playerEvent.getName();
                            playerEvent.broadcastMessage(String.format("%s is hosting a(n) %s in channel %d, use @joinevent to join!", player.getName(), eventName, playerEvent.getChannel().getId()));
                            break;
                        }
                        case "name": {
                            if (args.length() > 1) {
                                String name = args.concatFrom(1);
                                playerEvent.setName(name);
                                player.dropMessage("Event name changed to " + name);
                            } else {
                                player.sendMessage(6, "Current event name: '{}'", playerEvent.getName());
                            }
                            break;
                        }
                        case "sp":
                        case "spawnpoint": {
                            playerEvent.setSpawnPoint(player.getPosition());
                            player.dropMessage("Spawn point has been set to your position");
                            break;
                        }
                        case "gate": {
                            if (args.length() > 1) {
                                Integer time = args.parseNumber(1, int.class);
                                String error = args.getFirstError();
                                if (error != null) {
                                    player.dropMessage(5, error);
                                    return true;
                                }
                                playerEvent.setGateTime(time);
                                player.dropMessage(String.format("Event time is now set to %d seconds", time));
                            } else {
                                player.dropMessage(5, "You must specify a time (in seconds) to set your gate timer.");
                            }
                            break;
                        }
                        case "close": {
                            if (playerEvent.isOpen()) {
                                playerEvent.setOpen(false);
                                if (playerEvent.getGateTime() == 0) {
                                    // manual gate closing
                                    playerEvent.broadcastMessage("The gate is now closed");
                                } else {
                                    player.dropMessage("You have closed the gate");
                                }
                            } else {
                                player.dropMessage("The gate is already closed");
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
                                            player.dropMessage(5, "You must specify at least 1 username");
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
                                            player.dropMessage(5, "You must specify at least 1 username");
                                        }
                                        break;
                                    }
                                    case "list":
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
                                player.dropMessage(5, "Remove players from the winner list regardless of how many points they have via: '!event winners remove <usernames>");
                                player.dropMessage(5, "Append players to the winner list via: '!event winners add <usernames...>'");
                                player.sendMessage(5, "Player names are split with a space e.g.: '!event winners add {}'", player.getName());
                            }
                            break;
                        }
                    }
                }
            } else {
                player.dropMessage("Use: '!event new' to begin configuring your event.");
                player.dropMessage("Use: '!event help' for a list of relevant commands.");
            }
            return true;
        } else if (command.equals("dispel", "dispelm")) {
            if (command.equals("dispelm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    if (!players.isGM() || player.isDebug()) {
                        players.dispelDebuffs();
                    }
                }
            } else {
                if (args.length() == 1) {
                    MapleCharacter target = client.getChannelServer().getPlayerStorage().getPlayerByName(args.get(0));
                    if (target != null) {
                        target.dispelDebuffs();
                    } else {
                        player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                    }
                } else {
                    player.dispelDebuffs();
                }
            }
        } else if (command.equals("lock", "lockm")) {
            MobSkill skill = MobSkillFactory.getMobSkill(120, 1);
            if (command.equals("lockm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    if (!players.isGM() || player.isDebug()) {
                        player.giveDebuff(MapleDisease.SEAL, skill);
                    }
                }
            } else {
                if (args.length() > 0) {
                    for (int i = 0; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().getPlayerByName(username);
                        if (target != null && (!target.isGM() || player.isDebug())) {
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
                    Integer level = args.parseNumber(0, 2, int.class);
                    String error = args.getFirstError();
                    if (error != null) {
                        player.dropMessage(5, error);
                    }
                    MobSkill skill = MobSkillFactory.getMobSkill(132, level);
                    if (map) {
                        for (MapleCharacter players : player.getMap().getCharacters()) {
                            if (!players.isGM() || player.isDebug()) {
                                players.giveDebuff(MapleDisease.CONFUSE, skill);
                            }
                        }
                    } else {
                        for (int i = 1; i < args.length(); i++) {
                            String username = args.get(i);
                            MapleCharacter target = ch.getPlayerStorage().getPlayerByName(username);
                            if (target != null && (!target.isGM() || player.isDebug())) {
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
                        player.sendMessage(5, "Syntax: !{} <mode>", command.getName());
                    } else {
                        player.sendMessage(5, "Syntax: !{} <mode> <usernames>", command.getName());
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
                            MapleCharacter target = ch.getPlayerStorage().getPlayerByName(username);
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
                    player.dropMessage(5, "To remove the auto kill for this map, use: '!ak reset");
                }
                return true;
            }
            player.getMap().setAutoKillPosition(player.getPosition().getLocation());
            Point ak = player.getMap().getAutoKillPosition();
            player.sendMessage(6, "Auto kill location set to x:{}, y:{}", ak.x, ak.y);
        } else if (command.equals("bomb", "bombmap", "bombm")) {
            if (command.equals("bombmap", "bombm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    for (int i = -5; i < 5; i++) {
                        MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
                        if (bomb == null) {
                            player.dropMessage(5, "An error occurred");
                            return false;
                        }
                        Point pos = players.getPosition().getLocation();
                        pos.x += (i * 30);
                        player.getMap().spawnMonsterOnGroudBelow(bomb, pos);
                    }
                }
            } else {
                final int timeIndex = args.findArg("-time");
                Float time = args.parseNumber(timeIndex, 1.5f, float.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return false;
                } else if (timeIndex > 0) {
                    time = Math.max(0, time);
                    player.sendMessage("Bomb timer set to {}s", time);
                }
                if (args.length() == 0 || (args.length() == 2 && timeIndex > 0)) {
                    MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
                    if (bomb == null) {
                        player.dropMessage(5, "An error occurred");
                        return false;
                    }
                    bomb.getStats().getSelfDestruction().setRemoveAfter((int) (time * 1000f));
                    player.getMap().spawnMonsterOnGroudBelow(bomb, player.getPosition());
                } else {
                    for (int i = 0; i < args.length(); i++) {
                        MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
                        if (bomb == null) {
                            player.dropMessage(5, "An error occurred");
                            return false;
                        }
                        String username = args.get(i);
                        MapleCharacter target = player.getMap().getCharacterByName(username);
                        if (target != null) {
                            bomb.getStats().getSelfDestruction().setRemoveAfter((int) (time * 1000f));
                            target.getMap().spawnMonsterOnGroudBelow(bomb, target.getPosition());
                        } else {
                            player.sendMessage(5, "Unable to find any player named '{}'", username);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}
