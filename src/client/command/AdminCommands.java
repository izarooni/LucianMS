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
import net.server.channel.Channel;
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
		adminCommands.add("!pmob <id> <amount> - Spawn a permanent monster");
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
			} else if (command.equalsIgnoreCase("npc")) {
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
					} catch (NumberFormatException e) {
						player.dropMessage(5, "Please, only use numbers.");
					}
				}
				return true;
			} else if (command.equalsIgnoreCase("pnpc")) {
	            if (args.length == 2) {
	                int npcId;
	                try {
	                    npcId = Integer.parseInt(args[1]);
	                } catch (NumberFormatException e) {
	                    player.dropMessage(String.format("'%s' is not a number", args[0]));
	                    return false;
	                }
	                MapleNPC npc = MapleLifeFactory.getNPC(npcId);
	                npc.setPosition(player.getPosition());
	                npc.setCy(player.getPosition().y);
	                npc.setF(player.isFacingLeft() ? 0 : 1);
	                npc.setFh(0); // You set this to 0 to allow floating NPCs
	                npc.setRx0(player.getPosition().x - 50);
	                npc.setRx1(player.getPosition().x + 50);
	                for (Channel channel : player.getClient().getWorldServer().getChannels()) {
	                    if (channel.getMapFactory().isMapLoaded(player.getMapId())) {
	                        channel.getMapFactory().getMap(player.getMapId()).addMapObject(npc);
	                        channel.getMapFactory().getMap(player.getMapId()).broadcastMessage(MaplePacketCreator.spawnNPC(npc));
	                    }
	                }
	                try {
	                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO spawns (idd, f, fh, cy, rx0, rx1, type , x, y, mid, mobtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                        ps.setInt(1, npcId);
	                        ps.setInt(2, npc.getF());
	                        ps.setInt(3, npc.getFh());
	                        ps.setInt(4, npc.getCy());
	                        ps.setInt(5, npc.getRx0());
	                        ps.setInt(6, npc.getRx1());
	                        ps.setString(7, "n");
	                        ps.setInt(8, npc.getPosition().x);
	                        ps.setInt(9, npc.getPosition().y);
	                        ps.setInt(10, player.getMapId());
	                        ps.setInt(11, 1);
	                        ps.executeUpdate();
	                        player.dropMessage("Npc created");
	                    }
	                } catch (SQLException e) {
	                    player.dropMessage("An error occured while trying to insert this NPC in the database: " + e.getMessage());
	                }
	            }
	            return false;
	        } else if (command.equals("pmob")) {
	            if (args.length == 3) {
	                int mobId;
	                int amount;
	                try {
	                    mobId = Integer.parseInt(args[1]);
	                    amount = Integer.parseInt(args[2]);
	                } catch (NumberFormatException e) {
	                    player.dropMessage(String.format("'%s' or '%s' is not a number", args[0], args[1]));
	                    return false;
	                }
	                int mobTime = 0;
	                int xpos = player.getPosition().x;
	                int ypos = player.getPosition().y;
	                int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
	                for (int i = 0; i < amount; i++) {
	                    MapleMonster mob = MapleLifeFactory.getMonster(mobId);
	                    if (mob != null && !mob.getName().equals("MISSINGNO")) {
	                        mob.setPosition(player.getPosition());
	                        mob.setCy(ypos);
	                        mob.setRx0(xpos + 50);
	                        mob.setRx1(xpos - 50);
	                        mob.setFh(fh);
	                        try {
	                            Connection con = DatabaseConnection.getConnection();
	                            PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
	                            ps.setInt(1, mobId);
	                            ps.setInt(2, 0);
	                            ps.setInt(3, fh);
	                            ps.setInt(4, ypos);
	                            ps.setInt(5, xpos - 50);
	                            ps.setInt(6, xpos + 50);
	                            ps.setString(7, "m");
	                            ps.setInt(8, xpos);
	                            ps.setInt(9, ypos);
	                            ps.setInt(10, player.getMapId());
	                            ps.setInt(11, mobTime);
	                            ps.executeUpdate();
	                        } catch (SQLException e) {
	                            player.dropMessage("An error occured while trying to insert the mob into the database: " + e.getMessage());
	                        }
	                        player.getMap().addMonsterSpawn(mob, 0, -1);
	                    } else {
	                        player.dropMessage(String.format("The monster '%s' does not exist", args[0]));
	                        break;
	                    }
	                }
	            }
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
					for(int i = 1; i < args.length; i++) {
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