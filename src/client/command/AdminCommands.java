package client.command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ItemConstants;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

public class AdminCommands implements CommandPattern {

	// gm level = 2
	
	static ArrayList<String> adminCommands = new ArrayList<String>();
	{
		adminCommands.add("!item <id> <OPT=amount> - spawn an item, and optionally choose an amount");
		adminCommands.add("!drop <id> <OPT=amount> - drop an item on the ground, and optionally choose an amount");
		adminCommands.add("!spawn <monster> <OPT=amount> - spawn a monster, and optionally multiple of the same sort");
		adminCommands.add("!npc <id> - spawn a npc at your location");
		adminCommands.add("!pnpc <id> - spawn a permanent npc at your location");
		adminCommands.add("!mob <id> <OPT=amount> - Another way to spawn a monster");
		adminCommands.add("!pmob <id> - Spawn a permanent monster");
		adminCommands.add("!pos - Show the position you're currently at");
		adminCommands.add("!servermessage <message> change the server message");
		adminCommands.add("!shout <message> - show a message on everyones screen with text you typed");
		adminCommands.add("!map - Show information about the map you're currently in");
		adminCommands.add("!testdialog <dialogid> - test a npc dialog");
		adminCommands.add("!playernpc <player> <scriptId> - Create a player npc");
		adminCommands.add("!saveall - Save everything on the server");
	}

	@Override
	public boolean execute(MapleClient c, char header, String command, String[] args) {
		MapleCharacter player = c.getPlayer();
		if (command.startsWith("!")) {
			command = command.replace("!", "");

			if (command.equalsIgnoreCase("item") || command.equalsIgnoreCase("drop")) {
				if (args.length >= 2) {
					int amount = 1;
					int petid = -1;

					int id = Integer.parseInt(args[1]);

					if (ItemConstants.isPet(id)) {
						petid = MaplePet.createPet(id);
					}

					if (args.length >= 3) {
						amount = Integer.parseInt(args[2]);
					}

					if (command.equalsIgnoreCase("item")) {
						if(MapleInventoryManipulator.addById(c, id, (short) amount, player.getName(), petid, -1)) {
							player.dropMessage(6, "Put item inside of your inventory.");
						} else {
							player.dropMessage(5, "This item does not exist, please try again.");
						}
					} else {
						Item toDrop;

						if (MapleItemInformationProvider.getInstance()
								.getInventoryType(id) == MapleInventoryType.EQUIP) {
							toDrop = MapleItemInformationProvider.getInstance().getEquipById(id);
						} else {
							toDrop = new Item(id, (byte) 0, (short) amount);
						}
						if(!(toDrop == null)) {
							player.dropMessage(5, "Dropped the item on the floor.");
							player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
						} else {
							player.dropMessage(5, "This item does not exist, please try again.");
						}
					}

				} else {
					player.dropMessage(5, "You need to specify an ID of the item you want.");
				}
				return true;
			} else if (command.equalsIgnoreCase("spawn")) {
				if (args.length >= 2) {
					int amount = 1;
					int monsterId = Integer.parseInt(args[1]);

					if (args.length >= 3)
						amount = Integer.parseInt(args[2]);
					MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
					if (amount >= 1) {
						for (int i = 0; i < amount; i++) {
							player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
						}
						return true;
					} else {
						player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
						return true;
					}
				} else {
					player.dropMessage(5, "You need to specify a monster id to spawn");
				}
				return true;
			} else if (command.equalsIgnoreCase("npc") || command.equalsIgnoreCase("pnpc")) {
				if (args.length >= 2) {
					try {
						int npcid = Integer.parseInt(args[1]);

						MapleNPC npc = MapleLifeFactory.getNPC(npcid);
						npc.setPosition(player.getPosition());
						npc.setCy(player.getPosition().y);
						npc.setRx0(player.getPosition().x + 50);
						npc.setRx1(player.getPosition().x - 50);
						npc.setFh(player.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
						player.getMap().addMapObject(npc);
						player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
						
						if (command.equalsIgnoreCase("pnpc")) {
							try (Connection con = DatabaseConnection.getConnection();
									PreparedStatement insert = con.prepareStatement(
											"INSERT INTO spawns (idd, f, fh, type, cy, rx0, rx1, x, y, mobtime, mid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
								insert.setInt(1, npcid);
								insert.setInt(2, npc.getF());
								insert.setInt(3, npc.getFh());
								insert.setString(4, "n");
								insert.setInt(5, npc.getCy());
								insert.setInt(6, npc.getRx0());
								insert.setInt(7, npc.getRx1());
								insert.setInt(8, (int) player.getPosition().getX());
								insert.setInt(9, (int) player.getPosition().getY());
								insert.setInt(10, 0);
								insert.setInt(11, player.getMapId());

								insert.execute();
							} catch (SQLException e) {
								e.printStackTrace();
							} catch (NumberFormatException e) {
								player.dropMessage(5, "Please use numbers ONLY for the mob time");
							}
						}

					} catch (NumberFormatException e) {
						player.dropMessage(5, "Please, only use numbers.");
					}
				}
				return true;
			} else if (command.equalsIgnoreCase("mob") || command.equalsIgnoreCase("pmob")) {
				if (args.length >= 2) {
					try {
						int amount = 1;
						int monsterid = Integer.parseInt(args[1]);

						if (args.length >= 3) {
							amount = Integer.parseInt(args[2]);
						}

						MapleMonster monster = player.getMap().getMonsterById(monsterid);
						if(monster != null) {
						if (amount > 1) {
							for (int i = 0; i < amount; i++) {
								player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
							}
						}

						if (command.equalsIgnoreCase("pmob")) {
							try (Connection con = DatabaseConnection.getConnection();
									PreparedStatement insert = con.prepareStatement(
											"INSERT INTO spawns (idd, f, fh, type, cy, rx0, rx1, x, y, mobtime, mid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
								insert.setInt(1, monster.getId());
								insert.setInt(2, monster.getF());
								insert.setInt(3, monster.getFh());
								insert.setString(4, "m");
								insert.setInt(5, monster.getCy());
								insert.setInt(6, monster.getRx0());
								insert.setInt(7, monster.getRx1());
								insert.setInt(8, (int) player.getPosition().getX());
								insert.setInt(9, (int) player.getPosition().getY());
								insert.setInt(10, args[3] == null ? 330 : Integer.parseInt(args[3]));
								insert.setInt(11, player.getMapId());

								insert.execute();
							} catch (SQLException e) {
								e.printStackTrace();
							} catch (NumberFormatException e) {
								player.dropMessage(5, "Please use numbers ONLY for the mob time");
							}
						}
						} else {
							player.dropMessage(5, "A monster with this id does not exist.");
						}
					} catch (NumberFormatException e) {
						player.dropMessage(5, "Please, only use numbers.");
					}
				} else {
					player.dropMessage(5, "Correct usage: !pmob <monsterid> <OPT=amount>");
				}
				return true;
			} else if(command.equalsIgnoreCase("playernpc")) {
                try {
                    if(args.length == 3) {
                      int scriptId = Integer.parseInt(args[2]);
                      MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(args[1]);
                      if(scriptId >= 9901000 && scriptId <= 9901909) {
                          if(target != null) {
                              player.playerNPC(target, scriptId);
                          } else {
                              player.dropMessage(6, "The player you tried to make an NPC out of, does not exist or is offline.");
                          }
                      } else {
                          player.dropMessage(6, "Player NPCs script Ids need to be between 9901000 and 9901909");
                      }
                    } else {
                    	player.dropMessage(5, "Correct usage: !playernpc <user> <scriptid>");
                    }
                  } catch(NumberFormatException e) {
                      e.printStackTrace();
                  }
                return true;
              } else if (command.equalsIgnoreCase("pos")) {
				player.dropMessage(6, "X: " + player.getPosition().getX() + " Y: " + player.getPosition().getY());
				return true;
			} else if(command.equalsIgnoreCase("servermessage")) {
				if(args.length >= 2) {
					StringBuilder builder = new StringBuilder();
					for(int i = 2; i < args.length; i++) {
						builder.append(args[i] + " ");
					}
					player.getClient().getChannelServer().broadcastGMPacket(MaplePacketCreator.serverMessage(builder.toString()));
					player.dropMessage(6, "Changed the server message.");
				} else {
					player.dropMessage(5, "Correct usage: !servermessage <message>");
				}
				return true;
			} else if(command.equalsIgnoreCase("shout")) {
				if(args.length >= 2) {
					StringBuilder builder = new StringBuilder();
					for(int i = 2; i < args.length; i++) {
						builder.append(args[i] + " ");
					}
				  player.getClient().getChannelServer().broadcastGMPacket(MaplePacketCreator.earnTitleMessage(builder.toString()));
			    }
				return true;
			} else if(command.equalsIgnoreCase("map")) {
				player.dropMessage(6, "Map id - " + player.getMapId());
				player.dropMessage(6, "Map name - " + player.getMap().getMapName());
				player.dropMessage(6, "Map street name - " + player.getMap().getStreetName());
				return true;
			} else if(command.equalsIgnoreCase("testdialog")) {
				if(args.length >= 2) {
					try {
					int dialog = Integer.parseInt(args[1]);
					 NPCScriptManager.getInstance().start(c, dialog, player);
					 player.dropMessage(6, "Opening the dialog..");
					} catch(NumberFormatException e) {
						player.dropMessage(5, "That is not a number!");
					}
					return true;
				}
				
			} else if(command.equalsIgnoreCase("saveall")) {
				for(MapleCharacter targets : player.getClient().getChannelServer().getPlayerStorage().getAllCharacters()) {
					targets.saveToDB();
				}
				player.dropMessage(6, "Successfully saved everything.");
			}
			
		}

		return false;
	}

}