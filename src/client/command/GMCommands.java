package client.command;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleJob;
import client.MapleStat;
import client.inventory.MapleInventoryType;
import net.server.Server;
import server.events.custom.Events;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class GMCommands implements CommandPattern {

	int tagRange = 5000;
	
	// gm level = 1

	ArrayList<String> commands = new ArrayList<String>();
	{
		commands.add("!help - to see what commands there are");
		commands.add("!commands - another way to see the commands");
		commands.add("!dc <player> | map - DC a player or the entire map");
		commands.add("!warp <mapid> - Warp to the specified map, by ID");
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
		commands.add("!tag - tag nearby players, range is determined by tagrange");
		commands.add("!tagrange - set the range for players to tag");
		commands.add("!revive <player|map> - Revive a player, or the entire map.");
		commands.add("!kill <player|map> - Kill a player, or the entire map");
		commands.add("!dc <player|map> - Disconnect a player from the game, or the entire map");
		commands.add("!reloadmap - Reload the map");
		commands.add("!killall - Kill all the monsters on the map");
		commands.add("!clearslot <inventory> <slot> - clear a slot in a specific inventory type");
	}

	@Override
	public boolean execute(MapleClient c, char header, String command, String[] args) {
		MapleCharacter player = c.getPlayer();
		if (command.startsWith("!")) {
			command = command.replace("!", "");

			// args.length 1 = command itself args.length > 2 = arguments
			if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("commands")) {
				if (player.gmLevel() >= 2) {
					commands.addAll(AdminCommands.adminCommands);
				}
				if (args.length == 2) {

					for (int i = (commands.size() > 10 ? Integer.parseInt(args[1]) * 10 : 0); i < (commands.size() > 10
							? Integer.parseInt(args[1]) * 10 + 10 : commands.size()); i++) {
						if (!(i > commands.size())) {
							player.dropMessage(6, commands.get(i));
						}
					}
				} else {
					for (int i = 0; i < commands.size() && i <= 10; i++) {
						player.dropMessage(6, commands.get(i));
					}
				}
				return true;
			} else if (command.equalsIgnoreCase("dc")) {
				if (args.length == 2) {
					if (args[1].equalsIgnoreCase("map")) {
						for (MapleCharacter toKick : player.getMap().getCharacters()) {
							if (!(toKick.gmLevel() > 0)) {
								toKick.getClient().disconnect(false, true);
							}
							player.dropMessage(6, "You succesfully disconnected all the regular players in the map.");
						}
					} else {
						MapleCharacter target = Server.getInstance().getWorld(player.getWorld()).getPlayerStorage()
								.getCharacterByName(args[1]);
						target.getClient().disconnect(false, true);
						player.dropMessage(6, "You sucesfully disconnected the user " + target.getName() + ".");
					}
				} else {
					player.dropMessage(5, "You need to specify a target.");

				}
				return true;
			} else if (command.equalsIgnoreCase("warp") || command.equalsIgnoreCase("goto")) {
				if (args.length >= 2) {
					int id = Integer.parseInt(args[1]);
					
					MapleMap map = Server.getInstance().getWorld(player.getWorld()).getChannel(player.getClient().getChannel())
							.getMapFactory().getMap(id);
					
					if (map != null && Server.getInstance().getWorld(player.getWorld()).getChannel(player.getClient().getChannel()).getMapFactory().isMapLoaded(id)) {
						player.changeMap(id);
						player.dropMessage(6, "Warped to the map " + player.getMap().getMapName());
					} else {
						player.dropMessage(5, "This map does not exist.");
					}
				} else {
					player.dropMessage(5, "Please specify a map ID to warp to.");
				}
				return true;
			} else if(command.equalsIgnoreCase("warpto")) {
				if(args.length >= 2) {
					String arg = args[1];
					
					MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(arg);
					if(target != null) {
						player.dropMessage(6, "Warping to " + target.getName());
						player.changeMap(target.getMapId());
					} else {
						player.dropMessage(5, "This player is not online, or does not exist.");
					}
					
				}
				return true;
			} else if (command.equalsIgnoreCase("mute")) {
				if(args.length >= 2) {
					MapleCharacter target = Server.getInstance().getWorld(player.getWorld()).getChannel(player.getClient().getChannel()).getPlayerStorage().getCharacterByName(args[1]);
					if(target != null) {
						target.setMuted(true);
						player.dropMessage(6, "You have muted " + target.getName());
						target.dropMessage(5, "You have been muted by " + player.getName());
					} else {
						player.dropMessage(5, "This player does not exist, or is not online.");
					}
				} else {
					player.dropMessage(5, "Correct usage: !mute <player>");
				}
				return true;
			} else if (command.equalsIgnoreCase("args")) {
				int amount = args.length;
				player.dropMessage(6, "Argument length: " + amount);
			} else if (command.equalsIgnoreCase("job")) {
				if(args.length >= 2) {
					player.dropMessage(6,"You changed your job to " + MapleJob.getById(Integer.parseInt(args[1])).name().toLowerCase());
					player.setJob(MapleJob.getById(Integer.parseInt(args[1])));
					player.updateSingleStat(MapleStat.JOB, Integer.parseInt(args[1]));
				} else {
					
				}
				return true;
			} else if (command.equalsIgnoreCase("str") || command.equalsIgnoreCase("dex")
					|| command.equalsIgnoreCase("luk") || command.equalsIgnoreCase("int")) {
				if (args.length == 2) {
					short statToIncrease = Short.parseShort(args[1]);

					switch (command.toLowerCase()) {
					case "str":
						if (!(player.getStr() + statToIncrease > 32767)) {
							player.setStr(player.getStr() + statToIncrease);
							player.updateSingleStat(MapleStat.STR, player.getStr() + statToIncrease);
							player.dropMessage(6, "You updated your strength.");
						} else {
							int available = 32767 - player.getStr();
							player.dropMessage(5, "You can add another " + available + " str.");
						}
						break;
					case "dex":
						if (!(player.getDex() + statToIncrease > 32767)) {
							player.setDex(player.getDex() + statToIncrease);
							player.updateSingleStat(MapleStat.DEX, player.getDex() + statToIncrease);
							player.dropMessage(6, "You updated your dexterity.");
						} else {
							int available = 32767 - player.getDex();
							player.dropMessage(5, "You can add another " + available + " dex.");
						}

						break;
					case "int":
						if (!(player.getInt() + statToIncrease > 32767)) {
							player.setInt(player.getInt() + statToIncrease);
							player.updateSingleStat(MapleStat.INT, player.getInt() + statToIncrease);
							player.dropMessage(6, "You updated your intelligence.");
						} else {
							int available = 32767 - player.getInt();
							player.dropMessage(5, "You can add another " + available + " int.");
						}
						break;
					case "luk":
						if (!(player.getLuk() + statToIncrease > 32767)) {
							player.setLuk(player.getLuk() + statToIncrease);
							player.updateSingleStat(MapleStat.LUK, player.getLuk() + statToIncrease);
							player.dropMessage(6, "You updated your luck.");
						} else {
							int available = 32767 - player.getLuk();
							player.dropMessage(5, "You can add another " + available + " Luk.");
						}
						break;
					}
				}
				return true;
			} else if (command.equalsIgnoreCase("event")) {
				if (args.length >= 2) {
					switch (args[1].toLowerCase()) {
					case "right":
						if (args.length >= 3) {
							if (args[2].equalsIgnoreCase("map")) {
								for (int id : Events.getInstance().getParticipants().keySet()) {
									MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage()
											.getCharacterById(id);
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
								MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage()
										.getCharacterByName(args[2]);
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
						if (args.length >= 3) {
							if (args[2].equalsIgnoreCase("map")) {
								for (int id : Events.getInstance().getParticipants().keySet()) {
									MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage()
											.getCharacterById(id);
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
								MapleCharacter toMove = player.getClient().getChannelServer().getPlayerStorage()
										.getCharacterByName(args[2]);
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
						if (args.length >= 3) {
							if (args[2].equalsIgnoreCase("map")) {
								for (int id : Events.getInstance().getParticipants().keySet()) {
									MapleCharacter toBeStunned = player.getClient().getChannelServer()
											.getPlayerStorage().getCharacterById(id);
									if (toBeStunned.hasDisease(MapleDisease.STUN)) {
										toBeStunned.dispelDebuff(MapleDisease.STUN);
										toBeStunned.dropMessage(5, "You have been healed");
									} else {
										toBeStunned.giveDebuff(MapleDisease.STUN,
												MobSkillFactory.getMobSkill(123, 1000));
										toBeStunned.dropMessage(5, "You have been stunned");
									}
								}
							} else {
								MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage()
										.getCharacterByName(args[2]);
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
							player.dropMessage(5,
									"Correct usage: !event stun <map/player> - Stun all the participants, or a specific one.");
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
									player.dropMessage(6,
											"Starting the event in " + Events.getInstance().getTime() + " seconds");
								}
								Server.getInstance().getWorld(player.getWorld())
										.broadcastPacket(MaplePacketCreator.serverNotice(6,
												"An event with the name " + Events.getInstance().getEventTitle()
														+ " has started, join it using @Joinevent, you have "
														+ (Events.getInstance().getTime() == 0 ? Events.DEFAULT_TIME
																: Events.getInstance().getTime())
														+ " seconds to join."));
							} else {
								player.dropMessage(5, "Please create an event first using !event create <name>");
							}
						} else {
							player.dropMessage(5, "An event is already ongoing");
						}

						break;
					case "create":
						if (args.length >= 3) {
							Events.getInstance().create(player, args[2]);
							player.dropMessage(6, "You created an event with the name " + args[2]);
							player.dropMessage(6, "You can now start the event by doing the following:");
							player.dropMessage(6,
									"Set the timer for the event (if you don't want to use a default time)");
							player.dropMessage(6, "Start the event by using !event start");
						}
						break;
					case "end":
						if (Events.getInstance().isActive()) {
							Events.getInstance().end();
							player.dropMessage(6,
									"Ending the event, winners will automatically be given their winnings if they won.");
						} else {
							player.dropMessage(5, "There is no active event");
						}
						break;
					case "winner":
						if (Events.getInstance().isActive()) {
							switch (args[2].toLowerCase()) {
							case "add":
								if (args.length >= 4) {
									MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage()
											.getCharacterByName(args[3]);
									if (target != null) {
										if (Events.getInstance().getParticipants().containsKey(target.getId())) {
											int amount = 1;
											if (args.length >= 5) {
												try {
													amount = Integer.parseInt(args[4]);
												} catch (NumberFormatException e) {
													player.dropMessage(5, "Please specify a number");
													return false;
												}
											}

											Events.getInstance().getParticipants().put(target.getId(),
													Events.getInstance().getParticipants().get(target.getId())
															+ amount);
											player.dropMessage(6, "You successfully added " + amount + " point(s) to "
													+ target.getName() + "'s event point balance");
											target.dropMessage(6, "You have received " + amount
													+ " event point(s), you will receive this when the event ends.");
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
								if (args.length >= 4) {
									MapleCharacter target = Server.getInstance().getWorld(player.getWorld())
											.getPlayerStorage().getCharacterByName(args[3]);
									if (target != null) {
											int amount = 1;
										if(args.length >= 5) {
											try {
												amount = Integer.parseInt(args[4]);
											} catch(NumberFormatException e) {
												player.dropMessage("Specify an amount in number form.");
												return false;
											}
										}
										if (Events.getInstance().getParticipants().containsKey(target.getId())
												&& Events.getInstance().getParticipants().get(target.getId()) >= amount) {
											Events.getInstance().getParticipants().put(target.getId(),
													Events.getInstance().getParticipants().get(target.getId()) - amount);
											player.dropMessage(6,
													"You successfully removed " + amount + " event point(s) from the player "
															+ target.getName());
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
								for(int id : Events.getInstance().getParticipants().keySet()) {
									if(Events.getInstance().getParticipants().get(id) >= 1) {
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
							if (args.length >= 3) {
								try {
									int time = Integer.parseInt(args[2]);
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
								if (args.length >= 3) {
									Events.getInstance().setTime(Integer.parseInt(args[2]));
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
							if (args.length >= 3) {
								MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage()
										.getCharacterByName(args[2]);
								if (target != null) {
									if (Events.getInstance().getParticipants().containsKey(target.getId())) {
										Events.getInstance().leaveEvent(target);
									} else {
										player.dropMessage(5,
												"This player is not a participator, so he cannot be kicked.");
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
				return true;
			} else if (command.equalsIgnoreCase("level")) {
				if (args.length >= 2) {
					try {
						int level = Integer.parseInt(args[1]);
						player.setLevel(level);
						player.updateSingleStat(MapleStat.LEVEL, level);
						player.dropMessage(6, "You successfully changed your level to " + level);
					} catch (NumberFormatException e) {
						player.dropMessage(5, "That is not a number!");
					}
				} else {
					player.dropMessage(5, "Correct usage: !level <level>");
				}
				return true;
			} else if (command.equalsIgnoreCase("ban")) {
				if(args.length >= 2) {
					MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args[1]);
					if(target != null) {
						if(args.length >= 3) {
							StringBuilder sb = new StringBuilder();
							for(int i = 2; i < args.length; i++) {
								sb.append(args[i] + " ");
							}
							target.ban(sb.toString());
						} else {
							target.ban("You have been banned.");
						}
						player.dropMessage(6, "You have sucessfully banned " + target.getName());
					} else {
						player.dropMessage(5, "This player does not exist, or is not online");
					}
				} else {
					player.dropMessage(5, "Correct usage: !ban <player> <OPT=reason>");
				}
				return true;
			} else if(command.equalsIgnoreCase("tagrange")) {
				if(args.length >= 2) {
					try {
						int newTagRange = Integer.parseInt(args[1]);
						player.dropMessage(6, "Changed the tag range to " + newTagRange);
					} catch(NumberFormatException e) {
						player.dropMessage(5, "Please insert numbers only");
					}
				}
				return true;
			} else if(command.equalsIgnoreCase("tag")) {
				if(args.length >= 1) {
					List<MapleCharacter> targetList = new ArrayList<>(player.getMap().getCharacters());
					for(MapleCharacter targets : player.getMap().getPlayersInRange(new Rectangle(tagRange / 100, tagRange / 100), targetList)) {
						if(targets != player) {
							targets.setHp(0);
							targets.setMp(0);
							targets.updateSingleStat(MapleStat.HP, 0);
							targets.updateSingleStat(MapleStat.MP, 0);
							targets.dropMessage(5, "You have been tagged!");
						}
					}
				}
				return true;
			} else if(command.equalsIgnoreCase("maxskills")) {
				player.maxSkills();
				player.dropMessage(6, "Your skills have been maxed.");
			} else if(command.equalsIgnoreCase("heal")) {
				if(args.length >= 1) {
					MapleCharacter target = null;
					if(args.length >= 2) {
						target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args[2]);
					}
					
					if(target == null) {
						player.setHpMp(player.getMaxHp());
						player.dropMessage(6, "You healed yourself.");
					} else {
						target.setHpMp(target.getMaxHp());
						player.dropMessage(6, "You healed " + target.getName() + ".");
					}
				}
				return true;
			} else if(command.equalsIgnoreCase("notice")) {
				if(args.length >= 2) {
					StringBuilder builder = new StringBuilder();
					for(int i = 2; i < args.length; i++) {
						builder.append(args[i] + " ");
					}
					player.getClient().getChannelServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, builder.toString()));
				} else {
					player.dropMessage(5, "Correct usage: !notice <message>");
				}
				return true;
			} else if(command.equalsIgnoreCase("gift")) {
				if(args.length >= 4) {
					MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args[2]);
					if(target != null) {
						int amount = Integer.parseInt(args[3]);
						if(target.addPoints(args[1], amount)) {
							player.dropMessage(6, "You have successfully given " + target.getName() + " " + amount + " " + args[3] + "!");
						} else {
							player.dropMessage(5, "The point type you specified, does not exist.");
						}
					} else {
						player.dropMessage(5, "This player does not exist, or is not online.");
					}
				} else {
					player.dropMessage(5, "Correct usage: !gift <pointtype> <player> <amount>");
				}
				return true;
			} else if(command.equalsIgnoreCase("revive")) {
				if(args.length >= 2) {
					String argument = args[1];
					if(argument != "map") {
						MapleCharacter target = Server.getInstance().getWorld(player.getWorld()).getChannel(player.getClient().getChannel()).getPlayerStorage().getCharacterByName(argument);
						if(target != null) {
							player.dropMessage(6, "Revived " + target.getName());
							target.setHp(target.getMaxHp());
							target.setMp(target.getMaxMp());
							target.updateSingleStat(MapleStat.HP, target.getMaxHp());
							target.updateSingleStat(MapleStat.MP, target.getMaxMp());
						}
					} else {
						player.dropMessage(6, "Revived the entire map.");
						for(MapleCharacter targets : player.getMap().getCharacters()) {
							targets.setHp(targets.getMaxHp());
							targets.setMp(targets.getMaxMp());
							targets.updateSingleStat(MapleStat.HP, targets.getMaxHp());
							targets.updateSingleStat(MapleStat.MP, targets.getMaxMp());
						}
					}
				}
				return true;
			} else if(command.equalsIgnoreCase("kill")) {
				if(args.length >= 2) {
					String argument = args[1];
					if(argument != "map") {
						MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(argument);
						if(target != null) {
							target.setHp(0);
							target.setMp(0);
							target.updateSingleStat(MapleStat.HP, 0);
							target.updateSingleStat(MapleStat.MP, 0);
						} else {
							player.dropMessage(5, "This player does not exist, or is not online.");
						}
					} else {
						for(MapleCharacter players : player.getMap().getCharacters()) {
							if(players != player && !(players.gmLevel() >= 1 && player.gmLevel() >= 4)) {
								players.setHp(0);
								players.setMp(0);
								players.updateSingleStat(MapleStat.HP, 0);
								players.updateSingleStat(MapleStat.MP, 0);
								player.dropMessage(6, "Sucessfully killed the players");
							}
						}
					}
					
				} else {
					player.dropMessage(6, "Correct usage: !kill <player|map>");
				}
				return true;
			} else if(command.equalsIgnoreCase("dc")) {
				if(args.length >= 2) {
					String argument = args[1];
					if(argument != "map") {
						MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(argument);
						if(target != null) {
							target.getClient().disconnect(true, true);
						} else {
							player.dropMessage(5, "This player is not online, or does not exist.");
						}
					} else {
						for(MapleCharacter players : player.getMap().getCharacters()) {
							if(players != player && !(players.gmLevel() >= 1)) {
									players.getClient().disconnect(true, true);
							}
						}
						player.dropMessage(6, "You have disconnected all players in this map.");
					}
				} else {
					player.dropMessage(5, "Correct usage: !dc <player|map>");
				}
				return true;
			} else if(command.equalsIgnoreCase("reloadmap")) {
				player.getClient().getChannelServer().getMapFactory().reloadField(player.getMapId());
				return true;
			} else if(command.equalsIgnoreCase("killall")) {
				for(MapleMonster monster : player.getMap().getMonsters()) {
					monster.killBy(player);
				}
				player.dropMessage(6, "Sucessfully killed all the monsters on the map.");
				return true;
			} else if(command.equalsIgnoreCase("clearslot")) {
				if(args.length >= 2) {
					try {
						String inventoryType = args[1];
						int slot = Integer.parseInt(args[2]);
						player.getInventory(MapleInventoryType.getByWZName(inventoryType)).removeItem((short)slot);
						player.dropMessage(6, "Cleared the slot");
					} catch(NumberFormatException e) {
						player.dropMessage(5, "Insert a slotnumber to remove");
					}
				} else {
					player.dropMessage(5, "Correct usage: !clearslot <inventorytype> <slot>");
				}
			}
		}
		return false;
	}

}
