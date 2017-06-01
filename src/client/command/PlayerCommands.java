package client.command;

import client.MapleCharacter;
import client.MapleClient;
import client.Relationship;
import client.Relationship.Status;
import net.server.channel.Channel;
import net.server.channel.handlers.RockPaperScissorsHandler;
import scripting.npc.NPCScriptManager;
import server.events.custom.GenericEvent;
import server.events.custom.ManualPlayerEvent;
import server.events.custom.auto.GAutoEvent;
import server.events.custom.auto.GAutoEventManager;
import server.events.pvp.FFA;
import server.events.pvp.PlayerBattle;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * @author izarooni, lucasdieswagger
 */
public class PlayerCommands {

    // TODO correct command argumentation, at every type of commands
    // TODO correct coloring depending on if it is an error message or not.

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
            commands.add("@jautoevent - join the auto event");
            commands.add("@lautoevent - leave the auto event");
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
            commands.add("@rps - Start a game of rock paper scissors vs a bot");
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("rates")) {
            player.dropMessage(6, "EXP rate: " + player.getExpRate());
            player.dropMessage(6, "Drop rate: " + player.getDropRate());
            player.dropMessage(6, "Meso rate: " + player.getMesoRate());
        } else if (command.equals("joinevent", "leaveevent")) {
            boolean join = command.equals("joinevent");
            ManualPlayerEvent playerEvent = client.getWorldServer().getPlayerEvent();
            if (playerEvent != null) {
                if (join) {
                    if (player.getMap() != playerEvent.getMap() && !playerEvent.participants.containsKey(player.getId())) {
                        ManualPlayerEvent.Participant p = new ManualPlayerEvent.Participant(player.getId(), player.getMapId());
                        playerEvent.participants.put(player.getId(), p);
                        player.changeMap(playerEvent.getMap(), playerEvent.getSpawnPoint());
                    } else {
                        player.dropMessage("You are already in the event!");
                    }
                } else {
                    ManualPlayerEvent.Participant p = playerEvent.participants.get(player.getId());
                    if (p != null) {
                        player.changeMap(p.returnMapId);
                    } else {
                        player.dropMessage("You are not in an event");
                    }
                }
            } else {
                player.dropMessage("There is no event going on right now");
            }
        } else if (command.equals("jautoevent", "lautoevent")) {
            boolean join = command.equals("jautoevent");
            GAutoEvent event = GAutoEventManager.getCurrentEvent();
            if (event != null) {
                boolean registered = event.isPlayerRegistered(player);
                if (join) {
                    if (registered) {
                        player.dropMessage("You are already in this auto event");
                    } else {
                        event.registerPlayer(player);
                    }
                } else {
                    if (registered) {
                        event.unregisterPlayer(player);
                    } else {
                        player.dropMessage("You are not in the auto event");
                    }
                }
            } else {
                player.dropMessage("There is not auto event going on right now");
            }
        } else if (command.equals("points")) {
            player.dropMessage(6, "Fishing Points: " + player.getFishingPoints());
            player.dropMessage(6, "Vote Points: " + player.getClient().getVotePoints());
            player.dropMessage("Event points: " + player.getEventPoints());
            player.dropMessage(6, "Donation points: " + player.getClient().getDonationPoints());
            player.dropMessage(6, "Shadow points: " + player.getShadowPoints());
        } else if (command.equals("dispose")) {
            NPCScriptManager.dispose(client);
            player.getClient().removeClickedNPC();
            player.announce(MaplePacketCreator.enableActions());
            player.dropMessage(6, "Disposed!");
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
                player.dropMessage(6, String.format("Channel(%d): %s", channel.getId(), sb.toString()));
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
                        if (player.getJQController() != null) {
                            player.setJQController(null);
                        }
                        player.changeMap(map);
                        return;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String s : maps.keySet()) {
                sb.append(s).append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.dropMessage(5, String.format("'%s' is an invalid map. Try one of the following:", args.get(0)));
            player.dropMessage(5, sb.toString());
            maps.clear();
        } else if (command.equals("cleardrops")) {
            player.getMap().clearDrops();
        } else if (command.equals("save")) {
            player.saveToDB();
            player.dropMessage(6, "Saved!");
        } else if (command.equals("callgm")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                if (!message.isEmpty()) {
                    client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, String.format("[GM_CALL] %s : %s", player.getName(), message)));
                    player.dropMessage(6, "Help message sent");
                } else {
                    player.dropMessage(5, "You must specify a message");
                }
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else if (command.equals("style", "styler", "stylist")) {
            NPCScriptManager.start(player.getClient(), 9900000, player);
        } else if (command.equals("report")) {
            if (args.length() > 1) {
                String username = args.get(0);
                String message = args.concatFrom(1);
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, String.format("[REPORT] %s : (%s) %s", player.getName(), username, message)));
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(5, "You must specify a username and message");
            }
        } else if (command.equals("pvp")) {
            Optional<GenericEvent> pvp = player.getGenericEvents().stream().filter(g -> (g instanceof PlayerBattle)).findFirst();
            if (pvp.isPresent()) {
                player.removeGenericEvent(pvp.get());
                player.dropMessage("You are no longer PvPing");
            } else {
                PlayerBattle battle = new PlayerBattle(player);
                player.addGenericEvent(battle);
                player.dropMessage("You are now PvPing");
            }
        } else if(command.equals("marry")) {
        	if(args.length() >= 1) {
        		Relationship playerRelation = player.getRelationship();
        		if(!(args.get(0).equals("deny"))) {
        			MapleCharacter target = ch.getPlayerStorage().getCharacterByName(args.get(0));
        			if(target != null) {
        				Relationship targetRelation = target.getRelationship();
        				if(!(target.getRelationship().getStatus() == Status.Engaged || target.getRelationship().getStatus() == Status.Married)) {
        					targetRelation.setBrideId(target.getId());
	        				targetRelation.setGroomId(target.getId());
	        				targetRelation.setStatus(Status.Engaged);
	        				
	        				playerRelation.setBrideId(target.getId());
	        				playerRelation.setGroomId(target.getId());
	        				playerRelation.setStatus(Status.Engaged);
	        				player.dropMessage(6, String.format("You requested to marry %s", target.getName()));
	        				target.dropMessage(6, String.format("%s has requested to marry you, type @marry %s to accept it or @marry deny to deny it.", player.getName(), player.getName()));
        				} else {
        					if(targetRelation.getGroomId() == player.getId() && targetRelation.getStatus() != Status.Married) {
        						targetRelation.setStatus(Status.Married);
        						playerRelation.setStatus(Status.Married);
        						player.dropMessage(6, String.format("Congratulations! You have married %s", target.getName()));
        						target.dropMessage(6, String.format("Congratulations! You have married %s", player.getName()));
        					} else {
        						player.dropMessage(6, String.format("%s is already married!", target.getName()));
        					}
        				}
        			} else {
        				player.dropMessage(6, "This person does not exist, or is not online");
        			}
        		} else {
        			if(player.getRelationship().getStatus() == Status.Engaged) {
        				player.getRelationship().setStatus(Status.Single);
        				MapleCharacter target = ch.getPlayerStorage().getCharacterById(player.getRelationship().getBrideId());
        				if(target != null) {
        					target.getRelationship().setStatus(Status.Single);
        					target.dropMessage(5, String.format("%s has denied your marriage request", player.getName()));
        				}
        				player.dropMessage(6, "You have denied the marriage request.");
        			}
        		}
        	}
        } else if(command.equals("rps")) {
        	RockPaperScissorsHandler.startGame(player);
        	player.dropMessage(6, "Let's play some rock paper scissors!");
        }
    }
}
