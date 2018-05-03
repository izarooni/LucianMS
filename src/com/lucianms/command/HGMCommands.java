package com.lucianms.command;

import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import client.inventory.*;
import constants.ItemConstants;
import net.server.Server;
import net.server.channel.Channel;
import com.lucianms.server.events.channel.RockPaperScissorsEvent;
import net.server.world.World;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.io.scripting.portal.PortalScriptManager;
import com.lucianms.io.scripting.reactor.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MapleNPC;
import server.maps.MapleFoothold;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author izarooni, lucasdieswagger
 */
public class HGMCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        MapleCharacter player = client.getPlayer();

        if (command.equals("hgmcommands")) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add("!item <id> [amount] - spawn an item, and optionally choose an amount");
            commands.add("!drop <id> [amount] - drop an item on the ground, and optionally choose an amount");
            commands.add("!respawn - Respawn monsters and reset reactors in the current map");
            commands.add("!spawn <monster> [amount] - spawn a monster, and optionally multiple of the same sort");
            commands.add("!npc <id> - spawn a npc at your location");
            commands.add("!pnpc <id> - spawn a permanent npc at your location");
            commands.add("!pmob <id> <amount> - Spawn a permanent monster");
            commands.add("!playernpc <player> <scriptId> - Create a player npc");
            commands.add("!pos - Show the position you're currently at");
            commands.add("!shout <message> - show a message on everyones screen with text you typed");
            commands.add("!whereami - Show information about the map you're currently in");
            commands.add("!onpc <npcId> - Remotely open any NPC");
            commands.add("!oshop <shopId> - Remotely open any shop");
            commands.add("!saveall - Save everything on the server");
            commands.add("!reloadportals - Reload portal scripts");
            commands.add("!reloadreactordrops - Reload reactor drops");
            commands.add("!reloadshops - Reload shop items");
            commands.add("!reloadskills - Relaod loaded skill data");
            commands.add("!reloadmobs - Reload mob data");
            commands.add("!resetreactors - Reset all reactors in the map");
            commands.add("!godmeup - Change values of all stats for all equips");
            commands.sort(String::compareTo);
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("item", "drop")) {
            if (args.length() > 0) {
                Integer id = args.parseNumber(0, int.class);
                Short quantity = args.parseNumber(1, (short) 1, short.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                int petId = -1;
                if (ItemConstants.isPet(id)) {
                    petId = MaplePet.createPet(id);
                }
                if (command.equals("item")) {
                    if (MapleInventoryManipulator.addById(client, id, quantity, player.getName(), petId, -1)) {
                        client.announce(MaplePacketCreator.getShowItemGain(id, quantity));
                    } else {
                        player.dropMessage(5, "This item does not exist, please try again.");
                    }
                } else {
                    Item toDrop;
                    if (ItemConstants.getInventoryType(id) == MapleInventoryType.EQUIP) {
                        toDrop = MapleItemInformationProvider.getInstance().getEquipById(id);
                    } else {
                        toDrop = new Item(id, (byte) 0, quantity);
                    }
                    if (toDrop != null) {
                        player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
                    } else {
                        player.dropMessage(5, "That item does not exist");
                    }
                }
            } else {
                player.dropMessage(5, "You need to specify the ID of the item you want.");
            }
        } else if (command.equals("respawn")) {
            player.getMap().respawn();
            player.sendMessage("Monsters respawned");
            player.getMap().resetReactors();
            player.sendMessage("Reactors reset");
        } else if (command.equals("spawn")) {
            if (args.length() > 0) {
                Integer monsterId = args.parseNumber(0, int.class);
                Integer amount = args.parseNumber(1, 1, int.class);
                Integer hp = args.parseNumber(args.findArg("hp"), int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                for (int i = 0; i < amount; i++) {
                    MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
                    if (monster == null) {
                        player.dropMessage(5, String.format("'%d' is not a valid monster", monsterId));
                        return;
                    }
                    if (hp != null) {
                        MapleMonsterStats stats = new MapleMonsterStats();
                        stats.setHp(hp);
                        monster.setOverrideStats(stats);
                    }
                    player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
                }
            } else {
                player.dropMessage(5, "You must specify a monster ID");
            }
        } else if (command.equals("npc", "pnpc")) { // !pnpc <npc_id> [script_name]
            if (args.length() > 0) {
                boolean permanent = command.equals("pnpc");
                Integer npcId = args.parseNumber(0, int.class);
                String script = (args.length() == 2) ? args.get(1) : null;
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                npc.setPosition(player.getPosition());
                npc.setCy(player.getPosition().y);
                npc.setRx0(player.getPosition().x + 50);
                npc.setRx1(player.getPosition().x - 50);
                npc.setF(player.isFacingLeft() ? 0 : 1);
                npc.setScript(script);
                npc.setFh(0);
                player.getMap().addMapObject(npc);
                player.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                if (permanent) {
                    for (Channel channel : player.getClient().getWorldServer().getChannels()) {
                        if (channel.isMapLoaded(player.getMapId())) {
                            channel.getMap(player.getMapId()).addMapObject(npc);
                            channel.getMap(player.getMapId()).broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                        }
                    }
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO spawns (idd, f, fh, cy, rx0, rx1, type , x, y, mid, mobtime, script) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, npc.getF());
                        ps.setInt(3, npc.getFh());
                        ps.setInt(4, npc.getCy());
                        ps.setInt(5, npc.getRx0());
                        ps.setInt(6, npc.getRx1());
                        ps.setString(7, "n");
                        ps.setInt(8, (int) npc.getPosition().getX());
                        ps.setInt(9, (int) npc.getPosition().getY());
                        ps.setInt(10, player.getMapId());
                        ps.setInt(11, 1);
                        ps.setString(12, script);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        player.dropMessage(5, "An error occurred");
                    }
                }
            } else {
                player.dropMessage(5, "You must specify an NPC ID");
            }
        } else if (command.equals("pmob")) {
            if (args.length() > 0) {
                Integer mobId = args.parseNumber(0, int.class);
                Integer amount = args.parseNumber(1, 1, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
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
                            ps.setInt(11, 0);
                            ps.executeUpdate();
                        } catch (SQLException e) {
                            player.dropMessage(5, "An error occurred while trying to insert the mob into the database: " + e.getMessage());
                        }
                        player.getMap().addMonsterSpawn(mob, 0, -1);
                    } else {
                        player.dropMessage(5, String.format("'%s' is an invalid monster", args.get(0)));
                        return;
                    }
                }
            } else {
                player.dropMessage(5, "You must specify a monster ID");
            }
        } else if (command.equals("playernpc")) {
            if (args.length() > 1) {
                Integer npcId = args.parseNumber(0, int.class);
                String username = args.get(1);
                String script = (args.length() == 3) ? args.get(2) : null;
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(username);
                if (target != null) {
                    if (npcId >= 9901000 && npcId <= 9901909) {
                        player.createPlayerNPC(target, npcId, script);
                    } else {
                        player.dropMessage(5, "Player NPCs ID must be between 9901000 and 9901909");
                    }
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(5, "Syntax: !playernpc <npc_id> <username> [script_name]");
            }
        } else if (command.equals("pos")) {
            player.dropMessage(player.getPosition().toString());
        } else if (command.equals("shout", "say")) {
            if (args.length() > 0) {
                if (command.equals("say")) {
                    String message = String.format("%s : %s", player.getName(), args.concatFrom(0));
                    client.getWorldServer().broadcastPacket(MaplePacketCreator.serverNotice(6, message));
                } else {
                    client.getWorldServer().broadcastPacket(MaplePacketCreator.earnTitleMessage(args.concatFrom(0)));
                }
            } else {
                player.dropMessage("You must enter a message");
            }
        } else if (command.equals("whereami")) {
            player.dropMessage(6, "Map id - " + player.getMapId());
            player.dropMessage(6, "Map name - " + player.getMap().getMapName());
            player.dropMessage(6, "Map street name - " + player.getMap().getStreetName());
        } else if (command.equals("oshop")) {
            if (args.length() == 1) {
                Integer shopId = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleShop shop = MapleShopFactory.getInstance().getShop(shopId);
                if (shop != null) {
                    shop.sendShop(client);
                } else {
                    player.dropMessage(5, "This shop doesn't exist");
                }
            }
        } else if (command.equals("onpc")) {
            if (args.length() == 1) {
                Integer npcId = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error == null && npcId < 9901000) {
                    NPCScriptManager.start(client, npcId);
                }
                NPCScriptManager.start(client, 10200, args.get(0));
            } else {
                player.dropMessage(5, "You must specify an NPC ID");
            }
        } else if (command.equals("saveall")) {
            for (World worlds : Server.getInstance().getWorlds()) {
                worlds.getPlayerStorage().getAllCharacters().forEach(MapleCharacter::saveToDB);
            }
            player.dropMessage(6, "All characters saved!");
        } else if (command.equals("resetreactors")) {
            player.getMap().resetReactors();
            player.dropMessage("Reactors reset");
        } else if (command.equals("reloadreactordrops")) {
            ReactorScriptManager.clearDrops();
            player.dropMessage(6, "Reactor drops reloaded");
        } else if (command.equals("reloadportals")) {
            PortalScriptManager.clearPortalScripts();
            player.dropMessage(6, "Portal scripts reloaded");
        } else if (command.equals("reloadshosps")) {
            MapleShopFactory.getInstance().clearCache();
            player.dropMessage(6, "Shops reloaded");
        } else if (command.equals("reloadskills")) {
            SkillFactory.loadAllSkills();
            player.dropMessage("Loaded all skills");
        } else if (command.equals("reloadmobs")) {
            MapleLifeFactory.clear();
            player.dropMessage(6, "Mobs reloaded");
        } else if (command.equals("test")) {
            RockPaperScissorsEvent.startGame(player);
        } else if (command.equals("sudo")) {
            if (args.length() > 1) {
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(args.get(0));
                if (target != null) {
                    String cmd = args.concatFrom(1);
                    CommandWorker.process(target.getClient(), cmd, true);
                }
            }
        } else if (command.equals("godmeup")) {
            if (args.length() > 0) {
                List<ModifyInventory> mods = new ArrayList<>();
                Short stat = args.parseNumber(0, short.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                for (Item item : player.getInventory(MapleInventoryType.EQUIPPED).list()) {
                    if (item instanceof Equip) {
                        Equip eq = (Equip) item;
                        if (args.length() > 2) {
                            for (int i = 2; i < args.length(); i++) {
                                eq.setStat(args.get(i), stat);
                            }
                        } else {
                            eq.setStr(stat);
                            eq.setDex(stat);
                            eq.setInt(stat);
                            eq.setLuk(stat);
                            eq.setMdef(stat);
                            eq.setWdef(stat);
                            eq.setAvoid(stat);
                            eq.setAcc(stat);
                            eq.setWatk(stat);
                            eq.setMatk(stat);
                        }
                        mods.add(new ModifyInventory(3, eq));
                        mods.add(new ModifyInventory(0, eq));
                    }
                }
                client.announce(MaplePacketCreator.modifyInventory(true, mods));
                player.equipChanged();
                mods.clear();
            }
        } else if (command.equals("footholds")) {
            final int itemId = 3990022;
            List<MapleFoothold> footholds = player.getMap().getFootholds().getFootholds();
            for (MapleFoothold foothold : footholds) {
                Item item = new Item(itemId, (short) 0, (short) 1);
                item.setObtainable(false);
                item.setOwner("fh_id:" + foothold.getId());
                Point position = new Point(foothold.getX1(), foothold.getY1());
                player.getMap().spawnItemDrop(player, player, item, position, true, true);
            }
            player.dropMessage("Don't forget to !cleardrops when you're done");
        }
    }
}