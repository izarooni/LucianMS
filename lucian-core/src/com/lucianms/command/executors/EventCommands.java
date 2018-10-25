package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleDisease;
import com.lucianms.command.CommandWorker;
import com.lucianms.constants.ServerConstants;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.lang.GProperties;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.*;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
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
                                player.dropMessage(5, "You must specify a time (in seconds) to set your gate timer");
                            }
                            break;
                        }
                        case "close": {
                            if (playerEvent.isOpen()) {
                                playerEvent.setOpen(false);
                                world.broadcastMessage(6, "[Event] The gates are now closed");
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
        } else if (command.equals("bod")) {
            if (args.length() == 2) {
                Integer lowHP = args.parseNumber(0, int.class);
                Integer maxHP = args.parseNumber(1, int.class);
                if (lowHP == null || maxHP == null) {
                    player.sendMessage(args.getFirstError());
                    return false;
                }
                MapleMonster monster = MapleLifeFactory.getMonster(9500365);
                if (monster != null) {
                    MapleMonsterStats stats = new MapleMonsterStats(monster.getStats());
                    stats.setExp(0);
                    stats.setHp(Randomizer.rand(lowHP, maxHP));
                    monster.setOverrideStats(stats);
                    monster.getListeners().add(new MonsterListener() {
                        @Override
                        public void monsterKilled(MapleCharacter player, int aniTime) {
                            if (player != null) {
                                monster.getMap().broadcastMessage(5, "Box killed by '{}'", player.getName());
                            }
                        }
                    });
                    player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
                    player.getMap().broadcastGMMessage(5, "Box spawned with {} HP", stats.getHp());
                } else {
                    player.sendMessage(5, "Invalid monster");
                }
            } else {
                player.sendMessage(5, "syntax: !{} <min health> <max health>", command.getName());
            }
            return true;
        } else if (command.equals("stun", "stunm")) {
            giveDebuff(player, command, args, MobSkillFactory.getMobSkill(123, 1));
            return true;
        } else if (command.equals("lock", "lockm")) {
            giveDebuff(player, command, args, MobSkillFactory.getMobSkill(120, 1));
            return true;
        } else if (command.equals("reverse", "rev", "reversem", "revm")) {
            giveDebuff(player, command, args, MobSkillFactory.getMobSkill(132, 1));
            return true;
        } else if (command.equals("seduce", "sed", "seducem", "sedm")) {
            giveDebuff(player, command, args, MobSkillFactory.getMobSkill(128, 1));
            return true;
        } else if (command.equals("dispel", "dispelm")) {
            if (command.equals("dispelm")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    if (!players.isGM() || player.isDebug()) {
                        players.dispelDebuffs();
                    }
                }
            } else {
                if (args.length() > 0) {
                    for (int i = 0; i < args.length(); i++) {
                        String s = args.get(i);
                        MapleCharacter target = player.getMap().getCharacterByName(s);
                        if (target != null) {
                            target.dispelDebuffs();
                        } else {
                            player.sendMessage("Unable to find any player named '{}'", s);
                        }
                    }
                } else {
                    player.dispelDebuffs();
                }
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
                final String action = args.get(0);
                GProperties<Point> akp = player.getMap().getAutoKillPositions();
                switch (action) {
                    case "left":
                    case "right":
                    case "up":
                    case "down":
                        Point location = player.getPosition().getLocation();
                        akp.put(action, location);
                        player.sendMessage(5, "Auto-kill {} position set to [{}, {}]", action, location.x, location.y);
                        return true;
                    case "reset":
                    case "clear":
                        akp.clear();
                        player.sendMessage(5, "Auto-kill positions cleared");
                        return true;
                }
            }
            player.sendMessage(5, "Available directions: left, right, up and down");
            player.sendMessage(5, "To clear all auto-kill positions use: !{} reset", command.getName());
            return false;
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
        } else if (command.equals("warpoxmiddle", "warpoxright", "warpoxleft")) {
            if (player.getMapId() != 109020001) {
                player.sendMessage(5, "You cannot use this command here");
                return false;
            }
            Collection<MapleCharacter> characters = new ArrayList<>(player.getMap().getCharacters());
            try {
                for (MapleCharacter players : characters) {
                    if (!players.isGM() || player.isDebug()) {
                        Point location = players.getPosition().getLocation();
                        if (location.x >= -142 && command.equals("warpoxright")) {
                            players.changeMap(ServerConstants.HOME_MAP);
                        } else if (location.x >= -307 && location.x <= -143 && command.equals("warpoxmiddle")) {
                            players.changeMap(ServerConstants.HOME_MAP);
                        } else if (location.x <= -308 && command.equals("warpoxleft")) {
                            players.changeMap(ServerConstants.HOME_MAP);
                        }
                    }
                }
            } finally {
                characters.clear();
            }
        } else if (command.equals("warpout")) {
            if (args.length() > 1) {
                Integer fieldID = args.parseNumber(0, int.class);
                if (fieldID == null) {
                    if (args.get(0).equalsIgnoreCase("home")) {
                        fieldID = ServerConstants.HOME_MAP;
                    } else {
                        player.sendMessage(5, args.getFirstError());
                        return false;
                    }
                }
                for (int i = 1; i < args.length(); i++) {
                    String s = args.get(i);
                    MapleCharacter target = player.getMap().getCharacterByName(s);
                    if (target != null) {
                        target.changeMap(fieldID);
                    } else {
                        player.sendMessage(5, "Unable to find any player named '{}'", s);
                    }
                }
            } else {
                player.sendMessage(5, "syntax: !{} <map> <usernames...>", command.getName());
            }
        }
        return false;
    }

    private static void giveDebuff(MapleCharacter player, CommandWorker.Command command, CommandWorker.CommandArgs args, MobSkill skill) {
        boolean map = command.getName().endsWith("m");
        MapleDisease disease;
        switch (skill.getSkillId()) {
            default:
                return;
            case 120:
                disease = MapleDisease.SEAL;
                break;
            case 121:
                disease = MapleDisease.DARKNESS;
                break;
            case 122:
                disease = MapleDisease.WEAKEN;
                break;
            case 123:
                disease = MapleDisease.STUN;
                break;
            case 124:
                disease = MapleDisease.CURSE;
                break;
            case 125:
                disease = MapleDisease.POISON;
                break;
            case 126:
                disease = MapleDisease.SLOW;
                break;
            case 128:
                disease = MapleDisease.SEDUCE;
                if (args.length() > 1) {
                    String direction = args.get(1);
                    if (direction.equalsIgnoreCase("right")) {
                        skill = MobSkillFactory.getMobSkill(128, 2);
                    } else if (direction.equalsIgnoreCase("left")) {
                        skill = MobSkillFactory.getMobSkill(128, 1);
                    } else if (direction.equalsIgnoreCase("down")) {
                        skill = MobSkillFactory.getMobSkill(128, 11);
                    } else {
                        player.sendMessage(5, "The only seduces available are 'left', 'right' and 'down'");
                        return;
                    }
                } else {
                    player.sendMessage(5, "syntax: !{} <duration (seconds)> <direction> {}", command.getName(), (map ? "" : "<usernames...>"));
                    return;
                }
                break;
            case 132:
                disease = MapleDisease.CONFUSE;
                break;
        }
        skill.setDuration(5000); // default duration
        if (map) {
            if (args.length() > 0) {
                Integer number = args.parseNumber(0, int.class);
                if (number == null) {
                    player.sendMessage(5, args.getFirstError());
                    return;
                }
                skill.setDuration(number * 1000);
            }
            for (MapleCharacter players : player.getMap().getCharacters()) {
                if (!players.isGM() || players.isDebug()) {
                    if (skill.getSkillId() == 128) { // seduce
                        players.setChair(0);
                        players.announce(MaplePacketCreator.cancelChair(-1));
                        players.getMap().broadcastMessage(players, MaplePacketCreator.showChair(players.getId(), 0), false);
                    }

                    players.giveDebuff(disease, skill);
                }
            }
            return;
        } else if (args.length() > 0) {

            Integer duration = args.parseNumber(0, int.class);
            if (duration == null) {
                player.sendMessage(5, args.getFirstError() + " - please specify a duration for the debuff");
                return;
            }
            skill.setDuration(duration * 1000);

            for (int i = (disease == MapleDisease.SEDUCE ? 2 : 1); i < args.length(); i++) {
                String s = args.get(i);
                MapleCharacter target = player.getMap().getCharacterByName(s);
                if (target != null) {
                    if (!target.isGM() || target.isDebug()) {
                        if (skill.getSkillId() == 128) { // seduce
                            target.setChair(0);
                            target.announce(MaplePacketCreator.cancelChair(-1));
                            target.getMap().broadcastMessage(target, MaplePacketCreator.showChair(target.getId(), 0), false);
                        }

                        target.giveDebuff(disease, skill);
                    } else {
                        player.sendMessage(5, "You cannot debuff the player '{}'", s);
                    }
                } else {
                    player.sendMessage(5, "Unable to find any player named '{}'", s);
                }
            }
            return;
        }
        player.sendMessage(5, "syntax: !{} [duration (seconds)] <usernames...>", command.getName());
        player.sendMessage(5, "example: '!{} 3 {}' - to stun for 3 seconds", command.getName(), player.getName());
    }
}
