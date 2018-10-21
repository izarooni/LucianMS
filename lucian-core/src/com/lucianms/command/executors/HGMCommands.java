package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleStat;
import com.lucianms.client.Skill;
import com.lucianms.client.inventory.*;
import com.lucianms.command.CommandWorker;
import com.lucianms.constants.ItemConstants;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.server.*;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleFoothold;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            commands.add("!godmeup - Change values of all stats for all equips");
            commands.add("!popup - Display a server wide notice");
            commands.add("!stalker - Open the player stalking NPC");
            commands.add("!setname - Change your username or another player");
            commands.sort(String::compareTo);
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("popup")) {
            if (args.length() > 0) {
                String content = args.concatFrom(0);
                client.getWorldServer().broadcastPacket(MaplePacketCreator.serverNotice(0, content));
            } else {
                player.sendMessage(5, "You must type a message!");
            }
        } else if (command.equals("setname")) {
            if (args.length() == 2) {
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getPlayerByName(args.get(0));
                if (target != null) {
                    String oldName = target.getName();
                    target.setName(args.get(0));
                    target.sendMessage(5, "Your name has been changed to '{}'", target.getName());
                    player.sendMessage("Changed '{}''s name to '{}'. Relog to make changes", oldName, target.getName());
                } else {
                    player.sendMessage(5, "Could not find any player named '{}'", args.get(0));
                }
            } else if (args.length() == 1) {
                player.setName(args.get(0));
                player.sendMessage("Your name has been changed to '{}'. Relog to make changes", player.getName());
            } else {
                player.sendMessage(5, "Syntax: !setname <username> - Change your own username");
                player.sendMessage(5, "Syntax: !setname <target> <username> - Change another player's username");
            }
        } else if (command.equals("clearskills")) {
            for (Map.Entry<Skill, MapleCharacter.SkillEntry> set : new HashMap<>(player.getSkills()).entrySet()) {
                Skill sk = set.getKey();
                MapleCharacter.SkillEntry entry = set.getValue();
                player.changeSkillLevel(sk, (byte) (sk.isHidden() ? -1 : 0), sk.isHidden() ? 0 : entry.masterlevel, entry.expiration);
            }
        } else if (command.equals("hpmp")) {
            if (args.length() == 1) {
                Integer value = args.parseNumber(0, int.class);
                if (value == null) {
                    player.sendMessage(5, args.getFirstError());
                    return;
                }
                player.setHp(value);
                player.setMp(value);
                player.setMaxHp(value);
                player.setMaxHp(value);
                player.updateSingleStat(MapleStat.MAXHP, value);
                player.updateSingleStat(MapleStat.MAXMP, value);
                player.updateSingleStat(MapleStat.HP, value);
                player.updateSingleStat(MapleStat.MP, value);
            }
        } else if (command.equals("debug")) {
            player.setDebug(!player.isDebug());
            player.sendMessage("Your debug mode is now {}", (player.isDebug() ? "enabled" : "disabled"));
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
            player.getMap().resetReactors();
            player.sendMessage("Monsters and reactors have respawned and reset");
        } else if (command.equals("spawn")) {
            if (args.length() > 0) {
                Integer monsterId = args.parseNumber(0, int.class);
                Integer amount = args.parseNumber(1, 1, int.class);
                Float hp = args.parseNumber(args.findArg("hp"), float.class);
                Float exp = args.parseNumber(args.findArg("exp"), float.class);
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
                    if (args.length() > 3) {
                        MapleMonsterStats stats = new MapleMonsterStats();
                        stats.setHp(hp == null ? monster.getHp() : hp.intValue());
                        if (exp != null && exp < 0) {
                            exp = monster.getExp() * Math.abs(exp);
                        }
                        stats.setExp(exp == null ? monster.getExp() : exp.intValue());
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
                    for (MapleChannel channel : player.getClient().getWorldServer().getChannels()) {
                        if (channel.isMapLoaded(player.getMapId())) {
                            channel.getMap(player.getMapId()).addMapObject(npc);
                            channel.getMap(player.getMapId()).broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                        }
                    }
                    try (Connection con = client.getChannelServer().getConnection();
                         PreparedStatement ps = con.prepareStatement("INSERT INTO spawns (idd, f, fh, cy, rx0, rx1, type , x, mid, mobtime, script) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, npc.getF());
                        ps.setInt(3, npc.getFh());
                        ps.setInt(4, npc.getCy());
                        ps.setInt(5, npc.getRx0());
                        ps.setInt(6, npc.getRx1());
                        ps.setString(7, "n");
                        ps.setInt(8, (int) npc.getPosition().getX());
                        ps.setInt(9, player.getMapId());
                        ps.setInt(10, 1);
                        ps.setString(11, script);
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
                    if (mob != null) {
                        mob.setPosition(player.getPosition());
                        mob.setCy(ypos);
                        mob.setRx0(xpos + 50);
                        mob.setRx1(xpos - 50);
                        mob.setFh(fh);
                        try (Connection con = client.getChannelServer().getConnection();
                             PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, mid, mobtime) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")) {
                            ps.setInt(1, mobId);
                            ps.setInt(2, 0);
                            ps.setInt(3, fh);
                            ps.setInt(4, ypos);
                            ps.setInt(5, xpos - 50);
                            ps.setInt(6, xpos + 50);
                            ps.setString(7, "m");
                            ps.setInt(8, xpos);
                            ps.setInt(9, player.getMapId());
                            ps.setInt(10, 1000);
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
                MapleCharacter target = player.getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
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
            player.dropMessage(6, "Map onUserEnter - " + player.getMap().getOnUserEnter());
            player.dropMessage(6, "Map onFirstUserEnter - " + player.getMap().getOnFirstUserEnter());
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
        } else if (command.equals("cnpc")) {
            byte mode = 1, type = 1;
            int selection = -1;
            if (args.length() > 0) {
                mode = args.parseNumber(0, byte.class);
                type = args.parseNumber(1, byte.class);
                selection = args.parseNumber(2, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.sendMessage(5, error);
                    return;
                }
            }
            NPCScriptManager.action(client, mode, type, selection);
        } else if (command.equals("onpc")) {
            if (args.length() == 1) {
                Integer npcId = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error == null && npcId < 9901000) {
                    NPCScriptManager.start(client, npcId);
                } else {
                    String script = args.get(0);
                    if (script.startsWith("`")) {
                        script = script.substring(1);
                    }
                    NPCScriptManager.start(client, 10200, script);
                }
            } else {
                player.dropMessage(5, "Usage: !onpc <npc_id/script>");
            }
        } else if (command.equals("saveall")) {
            for (MapleWorld worlds : Server.getWorlds()) {
                worlds.getPlayerStorage().getAllPlayers().forEach(MapleCharacter::saveToDB);
            }
            player.dropMessage(6, "All characters saved!");
        } else if (command.equals("resetreactors")) {
            player.getMap().resetReactors();
            player.dropMessage("Reactors reset");
        } else if (command.equals("sudo")) {
            if (args.length() > 1) {
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getPlayerByName(args.get(0));
                if (target != null) {
                    String cmd = args.concatFrom(1);
                    CommandWorker.process(target.getClient(), cmd, true);
                } else {
                    player.sendMessage("Unable to find any player named '{}'", args.get(0));
                }
            }
        } else if (command.equals("stalker")) {
            NPCScriptManager.start(client, 10200, "f_stalker");
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