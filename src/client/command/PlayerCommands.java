package client.command;

import java.util.ArrayList;
import java.util.HashMap;

import client.MapleCharacter;
import client.MapleClient;
import scripting.npc.NPCScriptManager;
import server.events.custom.Events;
import server.maps.MapleMap;
import tools.FilePrinter;
import tools.MaplePacketCreator;

public class PlayerCommands implements CommandPattern {

	ArrayList<String> commands = new ArrayList<String>();
	{
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
	}
	HashMap<String, Integer> gotoMap = new HashMap<String, Integer>();
	{
		gotoMap.put("fm", 910000000);
		gotoMap.put("henesys", 100000000);
		gotoMap.put("florina", 110000000);
		gotoMap.put("nautilus", 120000000);
		gotoMap.put("ereve", 130000000);
		gotoMap.put("rien", 140000000);
		gotoMap.put("orbis", 200000000);
		gotoMap.put("ludi", 220000000);
		gotoMap.put("aqua", 230000000);
		gotoMap.put("leafre", 240000000);
		gotoMap.put("mulung", 250000000);
		gotoMap.put("ariant", 260000000);
		gotoMap.put("timetemple", 270000000);
		gotoMap.put("ellin", 300000000);
		gotoMap.put("arcade", 970000000);
		
	}
	
	@Override
	public boolean execute(MapleClient c, char header, String command, String[] args) {
		MapleCharacter player = c.getPlayer();
		
		if(command.startsWith("@")) {
			command = command.replace("@", "");
			if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("commands")) {
				if (args.length > 1) {
					for (int i = args.length * 10; i < args.length * 10 + 10; i++) {
						if(!(i > commands.size())) {
							player.dropMessage(5, commands.get(i));
						}
					}
				} else {
					for (int i = 0; i < 10; i++) {
						if(!(i > commands.size())) {
							player.dropMessage(5, commands.get(i));
						}
					}
				}
				return true;
			} else if(command.equalsIgnoreCase("rates")) {
					player.dropMessage(5, "EXP rate: " + player.getExpRate());
					player.dropMessage(5, "Drop rate: " + player.getDropRate());
					player.dropMessage(5,  "Meso rate: " + player.getMesoRate());
					return true;
			} else if(command.equalsIgnoreCase("joinevent")) {
				Events.getInstance().joinEvent(player);
				return true;
			} else if(command.equalsIgnoreCase("leaveevent")) {
				Events.getInstance().leaveEvent(player);
				return true;
			} else if(command.equalsIgnoreCase("points")) {
				player.dropMessage(6, "Fishing Points: " + player.getFishingPoints());
				player.dropMessage(6, "Vote Points: " + player.getClient().getVotePoints());
				player.dropMessage("Event points: " + player.getEventPoints());
				player.dropMessage(6, "Donation points: " + 0);
				player.dropMessage(6, "Shadow points: " + 0);		
				return true;
			} else if(command.equalsIgnoreCase("dispose")) {
				NPCScriptManager.getInstance().dispose(player.getClient());
				player.getClient().announce(MaplePacketCreator.enableActions());
				player.getClient().removeClickedNPC();
				player.dropMessage(5, "You have been disposed.");
				return true;
			} else if(command.equalsIgnoreCase("achievements")) {
				player.getClient().announce(MaplePacketCreator.getNPCTalk(9040004, (byte) 0, "These are the currently available achievements, blue means they are unlocked, red is locked. \r\n\r\n" + player.getAchievements().getAll(), "00 00", (byte) 3));
				return true;
			} else if(command.equalsIgnoreCase("home")) {
				player.changeMap(240070101);
				player.dropMessage(6, "Warped to the home");
				return true;
			} else if(command.equalsIgnoreCase("online")) {
				String[] playersPerChannel = new String[3];
				for(MapleCharacter players : player.getClient().getChannelServer().getPlayerStorage().getAllCharacters()) {
					playersPerChannel[players.getClient().getChannel() - 1] += (player.getName() != null) ? player.getName() + "," : "";
				}
				
				for(int i = 0; i < playersPerChannel.length; i++) {
					player.dropMessage(6, "Players on channel " + (i + 1) + ": " + playersPerChannel[i].replace(',', ' '));
				}
				
				return true;
			} else if(command.equalsIgnoreCase("go")) {
				if(args.length >= 2) {
					String arg = args[1];
					if(!(arg.equalsIgnoreCase("list"))) {
						if(gotoMap.containsKey(arg.toLowerCase())) {
							player.changeMap(gotoMap.get(arg));
							player.dropMessage(6, "Have fun in (the) " + arg);
						} else {
							player.dropMessage(5, "You are not allowed to go to this map.");
						}
					} else {
						for(String key : gotoMap.keySet()) {
							MapleMap map = player.getClient().getChannelServer().getMapFactory().getMap(gotoMap.get(key));
							if(map != null && map.getMapName() != null) {
								player.dropMessage(6,  "@go " + key + " - warp to " + (map.getMapName().isEmpty() || map.getMapName() == null ? key : map.getMapName()));
							}
						}
					}
				} else {
					player.dropMessage(5, "Correct usage: @go <town name> or @go list");
				}
				return true;
			} else if(command.equalsIgnoreCase("cleardrops")) {
				player.getMap().clearDrops();
				player.dropMessage(6, "You succesfully cleared the drops");
				return true;
			} else if(command.equalsIgnoreCase("save")) {
				player.saveToDB();
				player.dropMessage("Sucessfully saved you.");
				return true;
			} else if(command.equalsIgnoreCase("callgm")) {
				StringBuilder sb = new StringBuilder();
				if(args.length >= 3) {
					MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args[1]);
					if(target != null) {
				for(int i = 2; i < args.length; i++) {
					sb.append(args[i] + " ");
				}
				for(MapleCharacter targets : player.getClient().getChannelServer().getPlayerStorage().getAllCharacters()) {
					if(targets.gmLevel() >= 1) {
						targets.dropMessage(6, "The user " + player.getName() + " requested help with the reason: ");
						targets.dropMessage(6, sb.toString());
					}
					FilePrinter.printError(FilePrinter.REPORTS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " reported  " + target.getName() + " with the reason: " + sb.toString() + "\r\n");
				}
				} else {
					player.dropMessage(5, "Please fill in a reason for contacting a Game Master.");
				}
				} else {
					player.dropMessage(5, "This player does not exist, or is not online.");
				}
				return true;
			} else if(command.equalsIgnoreCase("style") || command.equalsIgnoreCase("kin")) {
				NPCScriptManager.getInstance().start(player.getClient(), 9900000, player);
				player.dropMessage("Opened the styling npc..");
				return true;
			} else if(command.equalsIgnoreCase("report")) {
				if(args.length >= 2) {
					StringBuilder sb = new StringBuilder();
					
					for(int i = 1; i < args.length; i++) {
						sb.append(args[i] + " ");
					}
					player.dropMessage(6, "You succesfully reported the bug, the responsible staff will be notified");
					FilePrinter.printError(FilePrinter.REPORTS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " reported a bug:  " + sb.toString() + "\r\n");
					
				}
				return true;
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			} else if(command.equalsIgnoreCase("")) {
				
			}
		}
		return false;
	}



}
