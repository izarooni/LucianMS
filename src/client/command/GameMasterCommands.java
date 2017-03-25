package client.command;

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import server.MapleInventoryManipulator;
import server.events.custom.Events;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author izarooni, lucasdieswagger
 */
public class GameMasterCommands {

    public static int tagRange = 5000;

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();
        Channel ch = client.getChannelServer();

        if (command.equals("help", "commands")) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add("!help - to see what commands there are");
            commands.add("!commands - another way to see the commands");
            commands.add("!dc <player> | map - DC a player or the entire map");
            commands.add("!warp <mapid> - Warp to the specified map, by ID");
            commands.add("!warphere <player> - warp a player to your map");
            commands.add("!goto <mapid> - another way to warp to a map by ID");
            commands.add("!event help - view all the event commands");
            commands.add("!event winner remove <name> <OPT=amount> - remove a winner from the list, taking away all their possible points.");
            commands.add("!event winner clear - clear all the points from the participators that they could get");
            commands.add("!event winner add <name> <OPT=amount> - add an event point to the named participator.");
            commands.add("!event create <eventname> - create an event and specify a name for it");
            commands.add("!event start - start the event you have created.");
            commands.add("!event left <player/map> places all the participators or a specific one on the left of the map");
            commands.add("!event right <player/map> places all the participators or a specific one on the right of the map");
            commands.add("!event stun <player/map> - stun all the participators or a specific one ");
            commands.add("!event kick <name> - kick a participator from the event");
            commands.add("!event timer <time> - start a timer on the player screens that ticks down to 0");
            commands.add("!event countdown <time> - set the time for the event to start");
            commands.add("!heal <OPT=player> - Heal yourself, or a player.");
            commands.add("!notice <message> - Send a notice to the server");
            commands.add("!mute <player> - cancel a player from chatting");
            commands.add("!clock <time> - add a clock timer for an amount of seconds");
            commands.add("!tag - tag nearby players, range is determined by tagrange");
            commands.add("!tagrange - set the range for players to tag");
            commands.add("!revive <player|map> - Revive a player, or the entire map.");
            commands.add("!kill <player|map> - Kill a player, or the entire map");
            commands.add("!dc <player|map> - Disconnect a player from the game, or the entire map");
            commands.add("!reloadmap - Reload the map");
            commands.add("!killall - Kill all the monsters on the map");
            commands.add("!maxstats - max your stats");
            commands.add("!maxskills - max your skills");
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("dc")) {
            if (args.length() == 1) {
                String username = args.get(0);
                if (username.equalsIgnoreCase("map")) {
                    player.dropMessage("Disconnecting players in the map...");
                    for (MapleCharacter players : player.getMap().getCharacters()) {
                        if (!players.isGM()) {
                            players.getClient().disconnect(false, players.getCashShop().isOpened());
                        }
                    }
                    player.dropMessage(6, "Done!");
                } else {
                    MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(username);
                    if (target != null) {
                        target.getClient().disconnect(false, target.getCashShop().isOpened());
                    } else {
                        player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                    }
                }
            } else {
                player.dropMessage(5, "You must specify a username");
            }
        } else if (command.equals("map", "warp", "warpmap", "warpmapx", "wm", "wmx", "wh", "whx")) {
            if (args.length() > 0) {
                String username = args.get(0);
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(username);
                boolean exact = command.getName().endsWith("x");
                if (target != null && command.equals("warp", "wh", "whx")) { // !<warp_cmd> <username>
                    if (target.getClient().getChannel() != client.getChannel()) {
                        target.getClient().changeChannel(client.getChannel());
                    }
                    MapleMap map = player.getMap();
                    if (args.length() == 2) { // !<warp_cmd> <username> <target_map>
                        Long a1 = args.parseNumber(1);
                        if (a1 == null) {
                            player.dropMessage(args.getError(1));
                            return;
                        }
                        map = ch.getMapFactory().getMap(a1.intValue());
                    }
                    if (exact) {
                        target.changeMap(map, player.getPosition());
                    } else {
                        target.changeMap(map);
                    }
                } else if (command.equals("wm", "wmx")) {
                    MapleMap map;
                    if (args.length() == 1) { // !<warp_cmd> <map_ID>
                        Long a1 = args.parseNumber(0);
                        if (a1 == null) {
                            player.dropMessage(args.getError(0));
                            return;
                        }
                        map = ch.getMapFactory().getMap(a1.intValue());
                    } else { // !<warp_cmd> - rewarping players, typically for events
                        map = player.getMap();
                    }
                    for (MapleCharacter players : player.getMap().getCharacters()) {
                        if (exact) {
                            players.changeMap(map, player.getPosition());
                        } else {
                            players.changeMap(map);
                        }
                    }
                } else { // !<warp_cmd> <map_ID> (portal_ID)
                    // map, warp
                    Long a1 = args.parseNumber(0);
                    Long a2 = args.parseNumber(1);
                    String error = args.getError(0, 1);
                    if (a1 == null || error != null) {
                        player.dropMessage(args.getError(0));
                        return;
                    }
                    int mapId = a1.intValue();
                    int portal = (a2 == null) ? 0 : a2.intValue();
                    MapleMap map = ch.getMapFactory().getMap(mapId);
                    if (map == null) {
                        player.dropMessage(5, "That map doesn't exist");
                        return;
                    }
                    player.changeMap(map, map.getPortal(portal));
                }
            } else {
                player.dropMessage(5, "You must specify a map ID");
            }
        } else if(command.equals("warpmap")) {
        	if(args.length() == 1) {
        		try {
        			int warpTo = Integer.parseInt(args.get(0));
        			
        			player.getMap().getCharacters().forEach((target) -> {
        				target.changeMap(warpTo);
        				target.dropMessage(6, String.format("You have been warped to %s by %s", target.getMap().getStreetName(), player.getName()));
        			});
        			player.dropMessage("You have warped the map to " + player.getMap().getStreetName());
        		} catch(NumberFormatException e) {
        			player.dropMessage(5, "Please insert a number.");
        		}
        	}
        } else if(command.equals("clock")) {
        	if(args.length() == 1) {
        		try {
        	    int time = Integer.parseInt(args.get(0));
        	    player.getMap().broadcastMessage(MaplePacketCreator.getClock(time));
        	    player.dropMessage(6, String.format("You successfully added a timer with %s seconds", time));
        	    } catch(NumberFormatException e) {
        			player.dropMessage(5, "Please insert a time in seconds, in a numeric variable.");
        		}
        	} else {
        		
        	}
        } else if(command.equals("warphere", "wh")) {
        	if(args.length() >= 1) {
        		String username = args.get(0);
        		MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
        		if(target != null) {
        			target.changeMap(player.getMapId());
        			player.dropMessage(String.format("You moved %s to your current map.", username));
        			target.dropMessage(String.format("You have been moved to %s by %s", player.getMap().getStreetName(), player.getName()));
        		} else {
        			player.dropMessage(String.format("Could not find any player named '%s'", username));
        		}
        	} else {
        		player.dropMessage(5, "You must specify a username");
        	}
        } else if (command.equals("mute", "unmute")) {
            if (args.length() == 1) {
                boolean mute = command.equals("mute");
                String username = args.get(0);
                MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    target.setMuted(mute);
                    player.dropMessage(6, String.format("'%s' has been %s", username, mute ? "muted" : "unmuted"));
                    target.dropMessage(6, String.format("you have been %s", mute ? "muted" : "unmuted"));
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(6, "you must specify a username");
            }
        } else if (command.equals("job")) {
            if (args.length() == 1) {
                Long a1 = args.parseNumber(0);
                if (a1 == null) {
                    player.dropMessage(args.getError(0));
                    return;
                }
                MapleJob job = MapleJob.getById(a1.intValue());
                if (job != null) {
                    player.setJob(job);
                    player.updateSingleStat(MapleStat.JOB, job.getId());
                } else {
                    player.dropMessage(5, String.format("'%s' is an invalid job", args.get(0)));
                }
            } else {
                player.dropMessage(5, "You must specify a job ID");
            }
        } else if (command.equals("hp", "mp", "str", "dex", "int", "luk")) {
            Long a1 = args.parseNumber(0);
            if (a1 == null) {
                player.dropMessage(args.getError(0));
                return;
            }
            short amount = a1.shortValue();
            switch (command.getName().toLowerCase()) {
                case "str":
                    if (!(player.getStr() + amount > 32767)) {
                        player.setStr(player.getStr() + amount);
                        player.updateSingleStat(MapleStat.STR, player.getStr() + amount);
                        player.dropMessage(6, "You updated your strength.");
                    } else {
                        int available = 32767 - player.getStr();
                        player.dropMessage(5, "You can add another " + available + " str.");
                    }
                    break;
                case "dex":
                    if (!(player.getDex() + amount > 32767)) {
                        player.setDex(player.getDex() + amount);
                        player.updateSingleStat(MapleStat.DEX, player.getDex() + amount);
                        player.dropMessage(6, "You updated your dexterity.");
                    } else {
                        int available = 32767 - player.getDex();
                        player.dropMessage(5, "You can add another " + available + " dex.");
                    }

                    break;
                case "int":
                    if (!(player.getInt() + amount > 32767)) {
                        player.setInt(player.getInt() + amount);
                        player.updateSingleStat(MapleStat.INT, player.getInt() + amount);
                        player.dropMessage(6, "You updated your intelligence.");
                    } else {
                        int available = 32767 - player.getInt();
                        player.dropMessage(5, "You can add another " + available + " int.");
                    }
                    break;
                case "luk":
                    if (!(player.getLuk() + amount > 32767)) {
                        player.setLuk(player.getLuk() + amount);
                        player.updateSingleStat(MapleStat.LUK, player.getLuk() + amount);
                        player.dropMessage(6, "You updated your luck.");
                    } else {
                        int available = 32767 - player.getLuk();
                        player.dropMessage(5, "You can add another " + available + " Luk.");
                    }
                    break;
                case "hp":
                    if (!(player.getMaxHp() + amount > 30000)) {
                        player.setMaxHp(player.getMaxHp() + amount);
                        player.updateSingleStat(MapleStat.MAXHP, player.getMaxHp() + amount);
                        player.dropMessage(6, "You updated your hp.");
                    } else {
                        int available = 32767 - player.getMaxHp();
                        player.dropMessage(5, "You can add another " + available + " hp.");
                    }
                    break;
                case "mp":
                    if (!(player.getMaxMp() + amount > 30000)) {
                        player.setMaxMp(player.getMaxMp() + amount);
                        player.updateSingleStat(MapleStat.MAXMP, player.getMaxMp() + amount);
                        player.dropMessage(6, "You updated your mp.");
                    } else {
                        int available = 32767 - player.getMaxMp();
                        player.dropMessage(5, "You can add another " + available + " mp.");
                    }
                    break;
            }
        } else if (command.equals("event")) {
            if (args.length() > 1) {
                switch (args.get(0).toLowerCase()) {
                    case "right":
                        if (args.length() > 1) {
                            if (args.get(1).equalsIgnoreCase("map")) {
                                for (int id : Events.getInstance().getParticipants().keySet()) {
                                    MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage().getCharacterById(id);
                                    if (toMove != null) {
                                        if (!toMove.hasDisease(MapleDisease.SEDUCE)) {
                                            if (toMove.getChair() != 0) {
                                                toMove.announce(MaplePacketCreator.cancelChair(toMove.getChair()));
                                                toMove.setChair(0);
                                            }
                                            toMove.giveDebuff(MapleDisease.SEDUCE, MobSkillFactory.getMobSkill(128, 2));
                                        } else {
                                            toMove.dispelDebuff(MapleDisease.SEDUCE);
                                        }
                                    } else {
                                        player.dropMessage(5, "The player is not online, or does not exist.");
                                    }
                                }
                            } else {
                                MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args.get(1));
                                if (toMove != null) {
                                    if (Events.getInstance().getParticipants().containsKey(toMove.getId())) {
                                        if (!toMove.hasDisease(MapleDisease.SEDUCE)) {
                                            if (toMove.getChair() != 0) {
                                                toMove.announce(MaplePacketCreator.cancelChair(toMove.getChair()));
                                                toMove.setChair(0);
                                            }
                                            toMove.giveDebuff(MapleDisease.SEDUCE, MobSkillFactory.getMobSkill(128, 2));
                                        } else {
                                            toMove.dispelDebuff(MapleDisease.SEDUCE);
                                        }
                                    } else {
                                        player.dropMessage(5, "This player is not a participant of the event.");
                                    }

                                } else {
                                    player.dropMessage(5, "This player is not logged in, or does not exist");
                                }
                            }
                        }
                        break;
                    case "left":
                        if (args.length() > 1) {
                            if (args.get(1).equalsIgnoreCase("map")) {
                                for (int id : Events.getInstance().getParticipants().keySet()) {
                                    MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage().getCharacterById(id);
                                    if (toMove != null) {
                                        if (!toMove.hasDisease(MapleDisease.SEDUCE)) {
                                            toMove.giveDebuff(MapleDisease.SEDUCE, MobSkillFactory.getMobSkill(128, 1));
                                        } else {
                                            toMove.dispelDebuff(MapleDisease.SEDUCE);
                                        }
                                    } else {
                                        player.dropMessage(5, "The player is not online, or does not exist.");
                                    }
                                }
                            } else {
                                MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args.get(1));
                                if (toMove != null) {
                                    if (Events.getInstance().getParticipants().containsKey(toMove.getId())) {
                                        if (!toMove.hasDisease(MapleDisease.SEDUCE)) {
                                            toMove.giveDebuff(MapleDisease.SEDUCE, MobSkillFactory.getMobSkill(128, 1));
                                        } else {
                                            toMove.dispelDebuff(MapleDisease.SEDUCE);
                                        }
                                    } else {
                                        player.dropMessage(5, "This player is not a participant of the event.");
                                    }

                                } else {
                                    player.dropMessage(5, "This player is not logged in, or does not exist");
                                }
                            }
                        }
                        break;
                    case "stun":
                        if (args.length() > 1) {
                            if (args.get(1).equalsIgnoreCase("map")) {
                                for (int id : Events.getInstance().getParticipants().keySet()) {
                                    MapleCharacter toBeStunned = player.getClient().getChannelServer().getPlayerStorage().getCharacterById(id);
                                    if (toBeStunned.hasDisease(MapleDisease.STUN)) {
                                        toBeStunned.dispelDebuff(MapleDisease.STUN);
                                        toBeStunned.dropMessage(5, "You have been healed");
                                    } else {
                                        toBeStunned.giveDebuff(MapleDisease.STUN, MobSkillFactory.getMobSkill(123, 1000));
                                        toBeStunned.dropMessage(5, "You have been stunned");
                                    }
                                }
                            } else {
                                MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args.get(1));
                                if (target != null) {
                                    if (Events.getInstance().getParticipants().containsKey(target.getId())) {
                                        if (target.hasDisease(MapleDisease.STUN)) {
                                            target.dispelDebuff(MapleDisease.STUN);
                                            target.dropMessage(5, "You have been healed");
                                        } else {
                                            target.giveDebuff(MapleDisease.STUN, MobSkillFactory.getMobSkill(123, 1));
                                            target.dropMessage(5, "You have been stunned");
                                        }
                                    } else {
                                        player.dropMessage(5, "This player is not a participant of the event.");
                                    }
                                } else {
                                    player.dropMessage(5, "This player is not logged in, or does not exist");
                                }
                            }
                        } else {
                            player.dropMessage(5, "Correct usage: !event stun <map/player> - Stun all the participants, or a specific one.");
                        }
                        break;
                    case "start":
                        if (!Events.getInstance().isActive()) {
                            if (Events.getInstance().getEventTitle() != null) {
                                if (Events.getInstance().getTime() == 0) {
                                    Events.getInstance().countdown(Events.DEFAULT_TIME);
                                    player.dropMessage(6, "Starting the event in " + Events.DEFAULT_TIME + " seconds");
                                } else {
                                    Events.getInstance().countdown(Events.getInstance().getTime());
                                    player.dropMessage(6, "Starting the event in " + Events.getInstance().getTime() + " seconds");
                                }
                                Server.getInstance().getWorld(player.getWorld()).broadcastPacket(MaplePacketCreator.serverNotice(6, "An event with the name " + Events.getInstance().getEventTitle() + " has started, join it using @Joinevent, you have " + (Events.getInstance().getTime() == 0 ? Events.DEFAULT_TIME : Events.getInstance().getTime()) + " seconds to join."));
                            } else {
                                player.dropMessage(5, "Please create an event first using !event create <name>");
                            }
                        } else {
                            player.dropMessage(5, "An event is already ongoing");
                        }

                        break;
                    case "create":
                        if (args.length() > 1) {
                            Events.getInstance().create(player, args.concatFrom(1));
                            player.dropMessage(6, "You created an event with the name " + args.concatFrom(1));
                            player.dropMessage(6, "You can now start the event by doing the following:");
                            player.dropMessage(6, "Set the timer for the event (if you don't want to use a default time)");
                            player.dropMessage(6, "Start the event by using !event start");
                        }
                        break;
                    case "end":
                        if (Events.getInstance().isActive()) {
                            Events.getInstance().end();
                            player.dropMessage(6, "Ending the event, winners will automatically be given their winnings if they won.");
                        } else {
                            player.dropMessage(5, "There is no active event");
                        }
                        break;
                    case "winner":
                        if (Events.getInstance().isActive()) {
                            switch (args.get(1).toLowerCase()) {
                                case "add":
                                    if (args.length() > 2) {
                                        MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args.get(2));
                                        if (target != null) {
                                            if (Events.getInstance().getParticipants().containsKey(target.getId())) {
                                                int amount = 1;
                                                if (args.length() > 3) {
                                                    try {
                                                        amount = Integer.parseInt(args.get(3));
                                                    } catch (NumberFormatException e) {
                                                        player.dropMessage(5, "Please specify a number");
                                                        return;
                                                    }
                                                }

                                                Events.getInstance().getParticipants().put(target.getId(), Events.getInstance().getParticipants().get(target.getId()) + amount);
                                                player.dropMessage(6, "You successfully added " + amount + " point(s) to " + target.getName() + "'s event point balance");
                                                target.dropMessage(6, "You have received " + amount + " event point(s), you will receive this when the event ends.");
                                            } else {
                                                player.dropMessage(5, "This player is not a participant of the event.");
                                            }
                                        } else {
                                            player.dropMessage(5, "This player does not exist, or is not online");
                                        }
                                    } else {
                                        player.dropMessage(5, "Correct usage: !event winner add <name> <OPT=amount>");
                                    }

                                    break;
                                case "remove":
                                    if (args.length() > 2) {
                                        MapleCharacter target = Server.getInstance().getWorld(player.getWorld()).getPlayerStorage().getCharacterByName(args.get(2));
                                        if (target != null) {
                                            int amount = 1;
                                            if (args.length() > 3) {
                                                try {
                                                    amount = Integer.parseInt(args.get(3));
                                                } catch (NumberFormatException e) {
                                                    player.dropMessage("Specify an amount in number form.");
                                                    return;
                                                }
                                            }
                                            if (Events.getInstance().getParticipants().containsKey(target.getId()) && Events.getInstance().getParticipants().get(target.getId()) >= amount) {
                                                Events.getInstance().getParticipants().put(target.getId(), Events.getInstance().getParticipants().get(target.getId()) - amount);
                                                player.dropMessage(6, "You successfully removed " + amount + " event point(s) from the player " + target.getName());
                                                target.dropMessage(6, "You lost " + amount + " event point(s) for this event.");
                                            } else {
                                                player.dropMessage(5, "This player is not participating in this event.");
                                            }
                                        } else {
                                            player.dropMessage(5, "This player does not exist, or is not online.");
                                        }
                                    } else {
                                        player.dropMessage(5, "Correct usage: !event winner remove <name>");
                                    }
                                    break;
                                case "clear":
                                    for (int id : Events.getInstance().getParticipants().keySet()) {
                                        if (Events.getInstance().getParticipants().get(id) >= 1) {
                                            Events.getInstance().getParticipants().put(id, 0);
                                        }
                                    }
                                    player.dropMessage(6, "Cleared all winners.");
                                    break;
                            }
                        } else {
                            player.dropMessage(5, "There is no ongoing event");
                        }
                        break;
                    case "timer":
                        if (Events.getInstance().isActive()) {
                            if (args.length() > 1) {
                                try {
                                    int time = Integer.parseInt(args.get(1));
                                    player.getMap().addMapTimer(time);
                                    player.dropMessage(6, "Created a timer and set it to " + time + " seconds");
                                } catch (NumberFormatException e) {
                                    player.dropMessage(5, "Only numbers are allowed as argument.");
                                }
                            } else {
                                player.dropMessage(5, "Correct usage: !event timer <time>");
                            }
                        } else {
                            player.dropMessage(5, "There is no ongoing event");
                        }
                        break;
                    case "countdown":
                        try {
                            if (Events.getInstance().getEventTitle() != null) {
                                if (args.length() > 2) {
                                    Events.getInstance().setTime(Integer.parseInt(args.get(1)));
                                } else {
                                    player.dropMessage(5, "Correct usage: !event countdown <time>");
                                }
                            } else {
                                player.dropMessage(5, "Please create an event before giving it a countdown.");
                            }
                        } catch (NumberFormatException e) {
                            player.dropMessage(5, "Please specify a number");
                        }
                        break;
                    case "kick":
                        if (Events.getInstance().isActive()) {
                            if (args.length() > 1) {
                                MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args.get(1));
                                if (target != null) {
                                    if (Events.getInstance().getParticipants().containsKey(target.getId())) {
                                        Events.getInstance().leaveEvent(target);
                                    } else {
                                        player.dropMessage(5, "This player is not a participator, so he cannot be kicked.");
                                    }
                                } else {
                                    player.dropMessage(5, "The player is not online, or does not exist.");
                                }
                            } else {
                                player.dropMessage(5, "Correct usage: !event kick <name>");
                            }
                        } else {
                            player.dropMessage(5, "There is currently no ongoing event.");
                        }
                        break;
                    default:
                        player.dropMessage(6, "Invalid command, event commands can be seen by using: !event help");
                }
            } else {
                player.dropMessage(5, "Invalid arguments, use !event help to see the available commands");
            }
        } else if (command.equals("level")) {
            if (args.length() == 1) {
                Long a1 = args.parseNumber(0);
                if (a1 == null) {
                    player.dropMessage(args.getError(0));
                    return;
                }
                int level = a1.intValue();
                player.setLevel(level);
                player.updateSingleStat(MapleStat.LEVEL, level);
            } else {
                player.dropMessage(5, "You must specify a level");
            }
        } else if (command.equals("ban")) {
            if (args.length() > 1) {
                String username = args.get(0);
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    target.ban(args.concatFrom(1));
                    target.getClient().disconnect(false, target.getCashShop().isOpened());
                    player.dropMessage(6, String.format("'%s' has been banned", username));
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(5, "You must specify a username and a reason");
            }
        } else if (command.equals("tagrange")) {
            if (args.length() == 1) {
                Long a1 = args.parseNumber(0);
                if (a1 == null) {
                    player.dropMessage(args.getError(0));
                    return;
                }
                int range = a1.intValue();
                player.dropMessage(6, "Tag ranged changed to " + range);
            }
        } else if (command.equals("tag")) {
            ArrayList<MapleCharacter> players = new ArrayList<>(player.getMap().getCharacters());
            for (MapleCharacter chrs : player.getMap().getPlayersInRange(new Rectangle(tagRange / 100, tagRange / 100), players)) {
                if (chrs != player && !chrs.isGM()) {
                    chrs.setHpMp(0);
                    chrs.dropMessage(6, "You have been tagged!");
                }
            }
            players.clear();
        } else if (command.equals("maxskills")) {
            player.maxSkills();
            player.dropMessage(6, "Your skills are now maxed!");
        } else if (command.equals("heal", "healmap")) {
            if (command.equals("healmap")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    players.setHp(players.getMaxHp());
                    players.setMp(players.getMaxMp());
                    players.updateSingleStat(MapleStat.HP, players.getHp());
                    players.updateSingleStat(MapleStat.MP, players.getMp());
                }
            } else if (args.length() > 0) {
                for (int i = 0; i < args.length(); i++) {
                    String username = args.get(i);
                    MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                    if (target != null) {
                        target.setHp(target.getMaxHp());
                        target.setMp(target.getMaxMp());
                        target.updateSingleStat(MapleStat.HP, target.getHp());
                        target.updateSingleStat(MapleStat.MP, target.getMp());
                    }
                }
            } else {
                player.setHp(player.getMaxHp());
                player.setMp(player.getMaxMp());
                player.updateSingleStat(MapleStat.HP, player.getHp());
                player.updateSingleStat(MapleStat.MP, player.getMp());
                player.dropMessage(6, "Healed");
            }
        } else if (command.equals("notice")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                ch.broadcastPacket(MaplePacketCreator.serverNotice(6, message));
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else if (command.equals("gift")) {
            if (args.length() == 3) {
                String type = args.get(0);
                String username = args.get(1);
                Long a1 = args.parseNumber(2);
                if (a1 == null) {
                    player.dropMessage(args.getError(2));
                    return;
                }
                int amount = a1.intValue();
                MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    if (target.addPoints(type, amount)) {
                        player.dropMessage(6, target.getName() + " received " + amount + " " + type);
                    } else {
                        player.dropMessage(5, String.format("'%s' is an invalid point type", type));
                    }
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(5, "Syntax: !gift <point_type> <username> <amount>");
            }
        } else if (command.equals("kill", "killmap")) {
            if (command.equals("killmap")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    if (!players.isGM()) {
                        players.setHpMp(0);
                    }
                }
            } else {
                if (args.length() > 0) {
                    for (int i = 0; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                        if (target != null) {
                            target.setHpMp(0);
                        }
                    }
                } else {
                    player.dropMessage(5, "You must specify at least 1 username");
                }
            }
        } else if (command.equals("dc")) {
            if (args.length() > 0) {
                if (args.get(0).equalsIgnoreCase("map")) {
                    player.getMap().getCharacters().stream().filter(p -> !player.isGM()).forEach(p -> p.getClient().disconnect(false, p.getCashShop().isOpened()));
                } else {
                    if (args.length() > 0) {
                        for (int i = 0; i < args.length(); i++) {
                            String username = args.get(i);
                            MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                            if (target != null) {
                                target.getClient().disconnect(false, target.getCashShop().isOpened());
                            }
                        }
                    } else {
                        player.dropMessage(5, "You must specify at least 1 username");
                    }
                }
            }
        } else if (command.equals("reloadmap")) {
            for (Channel channel : client.getWorldServer().getChannels()) {
                channel.getMapFactory().reloadField(player.getMapId());
            }
        } else if (command.equals("killall")) {
            player.getMap().getMonsters().forEach(m -> m.killBy(player));
        } else if (command.equals("clearinv")) {
            if (args.length() == 1) {
                String sType = args.get(0);
                MapleInventoryType iType;
                if (sType.equalsIgnoreCase("eq") || sType.equalsIgnoreCase("equip")) {
                    iType = MapleInventoryType.EQUIP;
                } else if (sType.equalsIgnoreCase("use")) {
                    iType = MapleInventoryType.USE;
                } else if (sType.equalsIgnoreCase("setup")) {
                    iType = MapleInventoryType.SETUP;
                } else if (sType.equalsIgnoreCase("etc")) {
                    iType = MapleInventoryType.ETC;
                } else if (sType.equalsIgnoreCase("cash")) {
                    iType = MapleInventoryType.CASH;
                } else {
                    player.dropMessage(5, String.format("'%s' is not a valid inventory type. Use: equip, use, setup, etc or cash", sType));
                    return;
                }
                MapleInventory inventory = player.getInventory(iType);
                for (byte i = 0; i < inventory.getSlotLimit(); i++) {
                    Item item;
                    if ((item = inventory.getItem(i)) != null) {
                        int itemId = item.getItemId();
                        short quantity = (short) player.getItemQuantity(itemId, false);
                        if (iType == MapleInventoryType.EQUIP) {
                            quantity = 1;
                        }
                        if (ItemConstants.isPet(itemId)) {
                            if (item.getPetId() > -1) {
                                // maybe skip pets instead?
                                try {
                                    DatabaseConnection.getConnection().createStatement().execute("delete from pets where petid = " + item.getPet().getUniqueId());
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                        }
                        if (item instanceof Equip) {
                            if (((Equip) item).getRingId() > -1) {
                                // skip friendship, crush and wedding rings
                                continue;
                            }
                        }
                        MapleInventoryManipulator.removeById(client, iType, itemId, quantity, false, false);
                    }
                }
            } else {
                player.dropMessage(5, "Syntax: !clearslot <inventory_type>");
            }
        } else if (command.equals("hide")) {
            if (args.length() == 1) {
                Long a1 = args.parseNumber(0);
                if (a1 == null) {
                    player.dropMessage(args.getError(0));
                    return;
                }
                int hLevel = a1.intValue();
                if (hLevel < 1 || hLevel > player.gmLevel()) {
                    player.dropMessage(6, "You may only enter a value between " + 1 + " and " + player.gmLevel());
                    return;
                }
                player.setHidingLevel(hLevel);
                player.dropMessage(6, "Your hidden value is now " + player.getHidingLevel());
            } else {
                player.dropMessage(6, "Your hiding value is " + player.getHidingLevel());
            }
        } else if (command.equals("maxstats")) {
            player.setStr(32767);
            player.setDex(32767);
            player.setInt(32767);
            player.setLuk(32767);
            player.updateSingleStat(MapleStat.STR, 32767);
            player.updateSingleStat(MapleStat.DEX, 32767);
            player.updateSingleStat(MapleStat.INT, 32767);
            player.updateSingleStat(MapleStat.LUK, 32767);

            player.setMaxHp(30000);
            player.setMaxMp(30000);
            player.updateSingleStat(MapleStat.HP, 32767);
            player.updateSingleStat(MapleStat.MP, 32767);
            player.setHpMp(30000);

            player.setFame(32767);
            player.updateSingleStat(MapleStat.FAME, 32767);
        } else if (command.equals("debuff")) {
            player.getMap().getCharacters().forEach(MapleCharacter::dispelDebuffs);
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
        } else if (command.equals("reverse", "reversemap", "rev", "revm")) {
            boolean map = command.equals("reversemap", "revm");
            if (args.length() > 0) {
                MobSkill skill = MobSkillFactory.getMobSkill(132, 2);
                Long a1 = args.parseNumber(0);
                if (a1 == null) {
                    player.dropMessage(args.getError(1));
                    return;
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
                }
            } else {
                if (map) {
                    player.dropMessage(5, "Syntax: !" + command.getName() + " <mode>");
                } else {
                    player.dropMessage(5, "Syntax: !" + command.getName() + " <mode> <usernames>");
                }
            }
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
                    return;
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
        } else if (command.equals("online")) {
            for (Channel channel : client.getWorldServer().getChannels()) {
                StringBuilder sb = new StringBuilder();
                for (MapleCharacter players : channel.getPlayerStorage().getAllCharacters()) {
                    if (!players.isGM() || players.getHidingLevel() <= player.getHidingLevel()) {
                        sb.append(players.getName()).append(" ");
                    }
                }
                player.dropMessage(String.format("Channel(%d): %s", channel.getId(), sb.toString()));
            }
        } else if (command.equals("!!")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(2, String.format("[GM] %s : %s", player.getName(), message)));
            } else {
                player.dropMessage(5, "You must specify a message");
            }
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
        } else if (command.equals("characters")) {
            if (args.length() == 1) {
                String username = args.get(0);
                ArrayList<String> usernames = new ArrayList<>();
                // will this statement work? who knows
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("select name from characters where account id = (select accountid from characters where name = ?)")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.isBeforeFirst()) {
                            while (rs.next()) {
                                usernames.add(rs.getString("name"));
                            }
                        } else {
                            player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                            return;
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("characters: ");
                    usernames.forEach(s -> sb.append(s).append(", "));
                    sb.setLength(sb.length() - 2);
                    player.dropMessage(sb.toString());
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.dropMessage(5, "An error occurred");
                }
            } else {
                player.dropMessage(5, "You must specify a username");
            }
        } else if (command.equals("ak")) {
            if (args.length() == 1) {
                if (args.get(0).equalsIgnoreCase("reset")) {
                    player.getMap().setAutoKillPosition(null);
                    player.dropMessage(6, "Auto kill position removed");
                } else {
                    player.dropMessage(5, "Use < !ak reset > to remove auto kill");
                }
                return;
            }
            player.getMap().setAutoKillPosition(player.getPosition());
            Point ak = player.getMap().getAutoKillPosition();
            player.dropMessage(6, String.format("Auto kill position set to: x: %d, y: %d", ak.x, ak.y));
        } else if (command.equals("bomb", "bombmap", "bombm")) {
            MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
            if (bomb == null) {
                player.dropMessage(5, "An error occured");
                return;
            }
            int time = 2;
            if (args.length() > 0) {
                int i1;
                String username = args.get(0);
                MapleCharacter target = ch.getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    target.getMap().spawnMonsterOnGroudBelow(bomb, player.getPosition());
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
                if ((i1 = args.findArg("-t")) > -1) {
                    Long a1 = args.parseNumber(i1);
                    if (a1 == null) {
                        player.dropMessage(args.getError(i1));
                        return;
                    }
                    time = a1.intValue();
                }
            }
            if (time < 0) {
                time = 0;
            }
            bomb.getStats().selfDestruction().setRemoveAfter(time * 1000);
            player.getMap().spawnMonsterOnGroudBelow(bomb, player.getPosition());
        }
    }
}