package com.lucianms.command.executors;

import com.lucianms.client.*;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.command.CommandWorker;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.ServerConstants;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.server.ConcurrentMapStorage;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import com.lucianms.server.world.MapleWorld;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Database;
import tools.MaplePacketCreator;
import tools.Pair;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * @author izarooni, lucasdieswagger
 */
public class GameMasterCommands {


    private static int TagRange = 20000;

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();
        MapleChannel ch = client.getChannelServer();
        MapleWorld world = client.getWorldServer();

        if (command.equals("gmcommands")) {
            ArrayList<String> commands = new ArrayList<>();
            commands.add("!help - to see what commands there are");
            commands.add("!commands - another way to see the commands");
            commands.add("!dc <username> | map - DC a player or the entire map");
            commands.add("!warp <mapid> - Warp to the specified map, by ID");
            commands.add("!warphere <username> - warp a player to your map");
            commands.add("!goto <mapid> - another way to warp to a map by ID");
            commands.add("!heal [username] - Heal yourself, or a player.");
            commands.add("!levelup <username> <levels> - Levels the specified player X times");
            commands.add("!notice <message> - Send a notice to the server");
            commands.add("!mute <username> - cancel a player from chatting");
            commands.add("!clock <time> - add a clock timer for an amount of seconds");
            commands.add("!tag - tag nearby players, range is determined by tagrange");
            commands.add("!tagrange - set the range for players to tag");
            commands.add("!revive <player|map> - Revive a player, or the entire map.");
            commands.add("!kill <player|map> - Kill a player, or the entire map");
            commands.add("!dc <player|map> - Disconnect a player from the game, or the entire map");
            commands.add("!reloadmap - Reload the map");
            commands.add("!killall - Kill all the monsters on the map");
            commands.add("!cleardrops - Clears all drops in the map");
            commands.add("!maxstats - max your stats");
            commands.add("!maxskills - max your skills");
            commands.add("!hide - change who can see you in hide");
            commands.add("!sethide - toggles hide on/off, equivalent of using hide skill");
            commands.add("!clearinv <inventory> - clear all items in the specified inventory");
            commands.add("!debuff <usernames/map> - remove disease from specified players, or everybody in the map");
            commands.add("!lock <usernames> - prevent players from using skills");
            commands.add("!lockm - prevent all players in the map from using skills");
            commands.add("!reverse <usernames> - flip players movements");
            commands.add("!reversemap - flip movement of all players in the map");
            commands.add("!seduce <usernames> - force players to move in a direction");
            commands.add("!seducemap - force all players in the map to move in a direction");
            commands.add("!online - list all visible GMs and players online");
            commands.add("!gift <type> <username> <amount> - Give a player a certain type and specified amount of points");
            commands.add("!gift <type> <amount> <players..> - Give a multiple players a certain type of points");
            commands.add("!! <message> - sends a message to all GMs online");
            commands.add("!itemvac - Loot all item drops in the map");
            commands.add("!characters <username> - lists other characters that belong to a player");
            commands.add("!bomb [username] - spawns a bomb at the specified player location, or your location if no name provided");
            commands.add("!bombmap - spawns bombs everywhere");
            commands.add("!jail <username> <reason> - Jail a player");
            commands.add("!unjail <username> - Remove a player for jail");
            commands.add("!search <category> <name> - Search for a map, items, npcs or skills");
            commands.add("!chattype <type> - Change your general chat color");
            commands.add("!buff [username] - Buff yourself or a specified player");
            commands.add("!ap <amount> - Give yourself or another player AP");
            commands.add("!sp <amount> - Give yourself or another player SP");
            commands.add("!setall <number> [username] - Set all stats for yourself or a player");
            commands.add("!gender <username> <male/female/uni> - Change the gender of a specified player");
            commands.add("!stalker - Go through any player inventory");
            commands.add("!gmmap - Warps you to the GM headquarters");
            commands.add("!itemq <itemid> <morethan> - Check what online players have more than of an item id");
            commands.add("!killed - Find out who last died in the map.");
            commands.sort(String::compareTo);
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("fwarp")) {
            Integer fieldId = args.parseNumber(0, int.class);
            if (fieldId != null) {
                client.announce(MaplePacketCreator.getWarpToMap(fieldId, 0x80, player, null));
            } else {
                player.sendMessage(5, "Usage: !fwarp <map_id>");
            }
        } else if (command.equals("killed")) {
            if (player.getMap().getLastPlayerDiedInMap() != null) {
                player.dropMessage(6, "The last player who died on this map is " + player.getMap().getLastPlayerDiedInMap());
            } else {
                player.dropMessage(5, "Nobody died here.. yet. Perhaps you should try dying yourself?");
            }
        } else if (command.equals("itemq")) {
            if (args.length() >= 2) {
                StringBuilder sb = new StringBuilder();
                int itemId = args.parseNumber(0, int.class);
                int qnty = args.parseNumber(1, int.class);
                sb.append("Online users: \r\n\r\n");
                if (args.getFirstError() == null) {
                    for (MapleCharacter target : ch.getPlayerStorage().values()) {
                        int actualQuantity = target.getItemQuantity(itemId, true);
                        if (actualQuantity >= qnty && !target.isGM()) {
                            sb.append("account id ").append(target.getAccountID()).append(" | ")
                                    .append(target.getName()).append(" has ")
                                    .append(actualQuantity).append(" of ").append("#v")
                                    .append(itemId).append("#\r\n");
                        }
                    }

                    if (args.length() == 3 && Boolean.parseBoolean(args.get(2))) {
                        sb.append("\r\nOffline users: \r\n\r\n");
                        try (Connection con = client.getWorldServer().getConnection();
                             PreparedStatement stmt = con.prepareStatement(
                                     "SELECT c.accountid, c.name, SUM(ii.quantity) as 'quantity' FROM characters c" +
                                             " JOIN inventoryitems ii ON c.id = ii.characterid" +
                                             " WHERE itemid = ? AND c.gm < 1 AND" +
                                             " c.accountid NOT IN" +
                                             " (SELECT id FROM accounts ic WHERE banned = 1)" +
                                             " GROUP BY c.name HAVING SUM(ii.quantity) > ?"
                             )) {
                            stmt.setInt(1, itemId);
                            stmt.setInt(2, qnty);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    sb.append("account id ").append(rs.getInt("c.accountid")).append(" | ")
                                            .append(rs.getString("c.name")).append(" has ")
                                            .append(rs.getInt("quantity")).append(" of ").append("#v")
                                            .append(itemId).append("#\r\n");
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace(); // This aint it chief
                        }
                    }
                } else {
                    player.dropMessage(5, "Correct syntax: !itemq <itemid> <morethan>");
                    return;
                }
                if (!(sb.length() == 0)) {
                    client.announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, sb.toString(), "00 00", (byte) 0));
                    sb.setLength(0);
                } else {
                    player.dropMessage(5, "Nobody found online with more than this amount.");
                }
            } else {
                player.dropMessage(5, "Correct syntax: !itemq <itemid> <morethan>");
            }
        } else if (command.equals("stalker")) {
            NPCScriptManager.start(client, 10200, "t_stalker");
        } else if (command.equals("gmmap")) {
            player.changeMap(331001000, 0);
        } else if (command.equals("dc")) {
            if (args.length() == 1) {
                String username = args.get(0);
                if (username.equalsIgnoreCase("map")) {
                    player.dropMessage("Disconnecting players in the map...");
                    Collection<MapleCharacter> characters = new ArrayList<>(player.getMap().getCharacters());
                    try {
                        for (MapleCharacter players : characters) {
                            if (!players.isGM()) {
                                players.getClient().disconnect(false);
                            }
                        }
                    } finally {
                        characters.clear();
                    }
                    player.dropMessage(6, "Done!");
                } else {
                    MapleCharacter target = client.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        target.getClient().disconnect(false);
                    } else {
                        player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
                    }
                }
            } else {
                player.dropMessage(5, "You must specify a username");
            }
        } else if (command.equals("map", "warp", "warpmap", "warpmapx", "wm", "wmx", "wh", "whx")) {
            try {
                boolean exact = command.getName().endsWith("x");
                if (args.length() > 0) {
                    String username = args.get(0);
                    MapleCharacter target = client.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null && command.equals("warp", "wh", "whx")) { // !<warp_cmd> <username/map>
                        if (args.length() == 1 && command.equals("warp", "wh", "whx")) { // !<warp_cmd> <username>
                            MapleCharacter warpie = (command.equals("warp") ? player : target); // person to warp
                            MapleCharacter warper = (command.equals("warp") ? target : player); // destination

                            if (target.getClient().getChannel() != client.getChannel()) {
                                warpie.setPosition(warper.getPosition().getLocation());
                                warpie.getClient().changeChannel(warper.getClient().getChannel());
                                warpie.setMap(warper.getMap());
                            } else {
                                if (exact || command.equals("warp")) {
                                    warpie.changeMap(warper.getMap(), warper.getPosition());
                                } else {
                                    warpie.changeMap(warper.getMap());
                                }
                            }
                            return;
                        } else if (args.length() == 2) { // !<warp_cmd> <username> <map_ID>
                            Integer mapId = args.parseNumber(1, int.class);
                            String error = args.getFirstError();
                            if (error != null) {
                                if (args.get(1).equalsIgnoreCase("ox")) {
                                    mapId = 109020001;
                                } else if (args.get(1).equalsIgnoreCase("home")) {
                                    mapId = ServerConstants.HOME_MAP;
                                } else {
                                    player.dropMessage(5, error);
                                    return;
                                }
                            }
                            MapleMap map = target.getClient().getChannelServer().getMap(mapId);
                            if (map != null) {
                                target.changeMap(map);
                            } else {
                                player.dropMessage(5, "That is an invalid map");
                            }
                            return;
                        }
                    }
                }
                if (command.equals("warpmap", "wm", "wmx")) {
                    MapleMap map = player.getMap();
                    if (args.length() == 1) { // !<warp_cmd> <map_ID>
                        Integer mapId = args.parseNumber(0, int.class);
                        String error = args.getFirstError();
                        if (error != null) {
                            String target = args.get(0);
                            if (target.equalsIgnoreCase("here")) {
                                mapId = player.getMapId();
                            } else if (target.equalsIgnoreCase("ox")) {
                                mapId = 109020001;
                            } else if (target.equalsIgnoreCase("home")) {
                                mapId = ServerConstants.HOME_MAP;
                            } else {
                                player.dropMessage(5, error);
                                return;
                            }
                        }
                        map = ch.getMap(mapId);
                    }
                    if (map != null) {
                        Collection<MapleCharacter> characters = new ArrayList<>(player.getMap().getCharacters());
                        try {
                            for (MapleCharacter players : characters) {
                                if (!players.isGM() || players.isDebug()) {
                                    if (exact) {
                                        players.changeMap(map, player.getPosition());
                                    } else {
                                        players.changeMap(map);
                                    }
                                }
                            }
                        } finally {
                            characters.clear();
                        }
                    } else {
                        player.dropMessage(5, "That is an invalid map");
                    }
                } else { // !<warp_cmd> <map_ID> [portal_ID]
                    // map, warp
                    Integer mapId = args.parseNumber(0, int.class);
                    Integer portal = args.parseNumber(1, 0, int.class);
                    String error = args.getFirstError();
                    if (error != null) {
                        String target = args.get(0);
                        if (target.equalsIgnoreCase("ox")) {
                            mapId = 109020001;
                        } else if (target.equalsIgnoreCase("home")) {
                            mapId = ServerConstants.HOME_MAP;
                        } else {
                            player.dropMessage(5, error);
                            return;
                        }
                    }
                    MapleMap map = ch.getMap(mapId);
                    if (map == null) {
                        player.dropMessage(5, "That map doesn't exist");
                        return;
                    }
                    player.changeMap(map, map.getPortal(portal));
                }
            } catch (NullPointerException e) {
                player.dropMessage(5, "That map does not exist.");
            }
        } else if (command.equals("clock")) {
            if (args.length() == 1) {
                Integer time = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                player.getMap().broadcastMessage(MaplePacketCreator.getClock(time));
                player.dropMessage(6, String.format("You successfully added a timer with %s seconds", time));
                player.dropMessage(5, "Please insert a time in seconds, in a numeric variable.");
            } else {
                player.dropMessage(5, "You must specify a time");
            }
        } else if (command.equals("mutemap", "mutem", "unmutem", "unmutemap")) {
            boolean mute = command.equals("mutemap", "mutem");
            String muteText = (mute ? "muted" : "unmuted");
            for (MapleCharacter players : player.getMap().getCharacters()) {
                if (!players.isGM() || player.isDebug()) {
                    players.setMuted(mute);
                    players.sendMessage(5, "You have been {}", muteText);
                }
            }
            player.sendMessage(5, "The map has been {}", (mute ? "muted" : "unmuted"));
        } else if (command.equals("mute", "unmute")) {
            if (args.length() > 0) {
                boolean mute = command.equals("mute");
                String muteText = (mute ? "muted" : "unmuted");
                for (int i = 0; i < args.length(); i++) {
                    final String username = args.get(i);
                    MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        target.setMuted(mute);
                        player.sendMessage(6, "'{}' has been {}", target.getName(), muteText);
                        target.sendMessage(6, "You have been {}", muteText);
                    } else {
                        player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
                    }
                }
            } else {
                player.dropMessage(5, "you must specify a username");
            }
        } else if (command.equals("job")) {
            if (args.length() > 0) {
                Integer jobId = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleJob job = MapleJob.getById(jobId);
                if (job == null) {
                    player.dropMessage(String.format("'%s' is not a valid job", args.get(0)));
                    return;
                }
                if (args.length() > 1) {
                    for (int i = 1; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (target != null) {
                            target.setJob(job);
                            target.updateSingleStat(MapleStat.JOB, job.getId());
                        } else {
                            player.dropMessage(String.format("Unable to find any player named '%s'", username));
                        }
                    }
                } else {
                    player.setJob(job);
                    player.updateSingleStat(MapleStat.JOB, job.getId());
                }
            } else {
                player.dropMessage(5, "Syntax: !job <job_id> [username]");
            }
        } else if (command.equals("hp", "mp", "str", "dex", "int", "luk")) {
            Short amount = args.parseNumber(0, short.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
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
        } else if (command.equals("levelup")) {
            if (args.length() == 2) {
                Integer levels = args.parseNumber(1, int.class);
                if (levels == null) {
                    player.sendMessage(args.getFirstError());
                    return;
                }
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target != null) {
                    for (int i = 0; i < levels; i++) {
                        target.levelUp(false);
                    }
                    player.sendMessage(5, "'{}' has leveled up {} times", target.getName(), levels);
                } else {
                    player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                }
            } else if (args.length() == 0) {
                player.levelUp(false);
            } else {
                player.sendMessage(5, "syntax: !{} <username> <levels>", command.getName());
            }
        } else if (command.equals("level")) {
            if (args.length() > 0) {
                Integer level = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                if (args.length() > 1) {
                    for (int i = 1; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (target != null) {
                            target.setLevel(level);
                            target.setExp(0);
                            target.updateSingleStat(MapleStat.LEVEL, level);
                            target.updateSingleStat(MapleStat.EXP, 0);
                            target.dropMessage("Your level has been updated to " + target.getLevel());
                        } else {
                            player.dropMessage(String.format("Unable to find any player named '%s'", username));
                        }
                    }
                } else {
                    player.setLevel(level);
                    player.setExp(0);
                    player.updateSingleStat(MapleStat.LEVEL, level);
                    player.updateSingleStat(MapleStat.EXP, 0);
                }
            } else {
                player.dropMessage(5, "Syntax: !level <number> [usernames...]");
            }
        } else if (command.equals("ban")) {
            if (args.length() > 1) {
                String username = args.get(0);
                MapleCharacter target = client.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
                MapleCharacter.ban(username, args.concatFrom(1), false);
                if (target != null) {
                    target.getClient().disconnect(false);
                    player.sendMessage(6, "'{}' has been banned", username);
                } else {
                    player.sendMessage(6, "Offline banned '{}'", username);
                }
            } else {
                player.dropMessage(5, "You must specify a username and a reason");
            }
        } else if (command.equals("tagrange")) {
            if (args.length() == 1) {
                Integer range = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                TagRange = range;
                player.sendMessage("Tag range set to {}", TagRange);
            } else {
                player.sendMessage("The tag range is currently set to {}", TagRange);
            }
        } else if (command.equals("tag")) {
            for (MapleMapObject obj : player.getMap().getMapObjectsInRange(player.getPosition(), TagRange, Collections.singletonList(MapleMapObjectType.PLAYER))) {
                MapleCharacter chrs = (MapleCharacter) obj;
                if (chrs != player && !chrs.isGM()) {
                    chrs.setHpMp(0);
                    chrs.dropMessage(6, "You have been tagged!");
                }
            }
        } else if (command.equals("maxskills")) {
            player.maxSkills();
            player.dropMessage(6, "Your skills are now maxed!");
        } else if (command.equals("heal", "healmap")) {
            if (command.equals("healmap")) {
                for (MapleCharacter players : player.getMap().getCharacters()) {
                    players.setHp(Math.max(players.getMaxHp(), players.getCurrentMaxHp()));
                    players.setMp(Math.max(players.getMaxMp(), players.getCurrentMaxMp()));
                    players.updateSingleStat(MapleStat.HP, players.getHp());
                    players.updateSingleStat(MapleStat.MP, players.getMp());
                }
            } else if (args.length() > 0) {
                for (int i = 0; i < args.length(); i++) {
                    String username = args.get(i);
                    MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
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
                player.dispelDebuffs();
                player.dropMessage(6, "Healed");
            }
        } else if (command.equals("notice")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                ch.broadcastPacket(MaplePacketCreator.serverNotice(6, player.getName() + " : " + message));
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else if (command.equals("gift")) {
            if (args.length() == 3) {
                String type = args.get(0);
                String username = args.get(1);
                Integer amount = args.parseNumber(2, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    if (target.addPoints(type, amount)) {
                        player.dropMessage(6, target.getName() + " received " + amount + " " + type);
                    } else {
                        player.sendMessage(5, "'{}' is an invalid point type", type);
                    }
                } else {
                    player.sendMessage(5, "Unable to find any player named '{}'", username);
                }
            } else if (args.length() >= 3) {
                String type = args.get(0);
                Integer amount = args.parseNumber(1, int.class);
                for (int i = 2; i < args.length(); i++) {
                    String username = args.get(i);
                    String error = args.getFirstError();
                    if (error != null) {
                        player.dropMessage(5, error);
                        continue;
                    }
                    MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        if (target.addPoints(type, amount)) {
                            player.dropMessage(6, target.getName() + " received " + amount + " " + type);
                        } else {
                            player.sendMessage(5, "'{}' is an invalid point type", type);
                            break;
                        }
                    } else {
                        player.sendMessage(5, "Unable to find any player named '{}'", username);
                    }
                }

            } else {
                player.dropMessage(5, "Syntax: !gift <point_type> <username> <amount> OR");
                player.dropMessage(5, "Syntax: !gift <point_type> <amount> <usernames..>");
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
                        MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
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
                    ArrayList<MapleCharacter> characters = new ArrayList<>(player.getMap().getCharacters());
                    try {
                        characters.stream().filter(p -> !p.isGM()).forEach(p -> p.getClient().disconnect(false));
                    } finally {
                        characters.clear();
                    }
                } else {
                    if (args.length() > 0) {
                        for (int i = 0; i < args.length(); i++) {
                            String username = args.get(i);
                            MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                            if (target != null) {
                                target.getClient().disconnect(false);
                            }
                        }
                    } else {
                        player.dropMessage(5, "You must specify at least 1 username");
                    }
                }
            }
        } else if (command.equals("reloadmap")) {
            if (args.length() == 0) {
                for (MapleChannel channel : client.getWorldServer().getChannels()) {
                    channel.reloadMap(player.getMapId());
                }
            } else if (args.length() == 1) {
                Integer mapId = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    MapleCharacter target = client.getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                    if (target != null) {
                        mapId = target.getMapId();
                    } else {
                        return;
                    }
                }
                for (MapleChannel channel : client.getWorldServer().getChannels()) {
                    channel.reloadMap(mapId);
                }
                player.dropMessage("Map " + mapId + " reloaded!");
            } else {
                player.dropMessage(5, "Syntax: !reloadmap [map_ID]");
            }
        } else if (command.equals("killall")) {
            player.getMap().killAllMonsters();
            player.dropMessage("All monsters killed!");
        } else if (command.equals("cleardrops")) {
            player.getMap().clearDrops(player);
            player.dropMessage("Drops cleared!");
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
                for (byte i = 0; i <= inventory.getSlotLimit(); i++) {
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
                                try (Connection con = client.getWorldServer().getConnection()) {
                                    Database.executeSingle(con, "delete from pets where petid = ?", item.getPet().getUniqueId());
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
                player.dropMessage(5, "Syntax: !clearinv <inventory_type>");
            }
        } else if (command.equals("hide")) {
            if (args.length() == 1) {
                Integer hLevel = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                if (hLevel < 1 || hLevel > player.getGMLevel()) {
                    player.dropMessage(6, "You may only enter a value between " + 1 + " and " + player.getGMLevel());
                    return;
                }
                player.setHidingLevel(hLevel);
                player.dropMessage(6, "Your hidden value is now " + player.getHidingLevel());
            } else {
                player.dropMessage(6, "Your hiding value is " + player.getHidingLevel());
            }
        } else if (command.equals("sethide")) {
            player.toggleHide();
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
            player.updateSingleStat(MapleStat.MAXHP, 32767);
            player.updateSingleStat(MapleStat.MAXMP, 32767);
            player.setHpMp(30000);

            player.setFame(32767);
            player.updateSingleStat(MapleStat.FAME, 32767);
        } else if (command.equals("debuff")) {
            if (args.length() > 0) {
                if (args.length() == 1 && args.get(0).equalsIgnoreCase("map")) {
                    player.getMap().getCharacters().forEach(MapleCharacter::dispelDebuffs);
                } else {
                    for (int i = 0; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (target != null) {
                            target.dispelDebuffs();
                        }
                    }
                    player.dropMessage(6, "Done!");
                }
            } else {
                player.dropMessage(5, "Syntax: !debuff <usernames/map>");
            }
        } else if (command.equals("online")) {
            StringBuilder sb = new StringBuilder();
            for (MapleChannel channel : client.getWorldServer().getChannels()) {
                ConcurrentMapStorage<Integer, MapleCharacter> storage = channel.getPlayerStorage();
                sb.append("#echannel ").append(channel.getId()).append(" - ").append(storage.size()).append(" players#n\r\n");
                for (MapleCharacter players : storage.values()) {
                    if (!players.isGM() || players.getHidingLevel() <= player.getHidingLevel()) {
                        sb.append(players.getName()).append(", ");
                    }
                }
                if (sb.length() > 2) {
                    sb.setLength(sb.length() - 2);
                }
                sb.append("\r\n");
            }
            client.announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        } else if (command.equals("!")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(2, String.format("[GM] %s : %s", player.getName(), message)));
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else if (command.equals("itemvac")) {
            MapleMap.doItemVac(player, null,-1);
        } else if (command.equals("characters")) {
            if (args.length() == 1) {
                String username = args.get(0);
                ArrayList<String> usernames = new ArrayList<>();
                // will this statement work? who knows
                try (Connection con = client.getWorldServer().getConnection();
                     PreparedStatement ps = con.prepareStatement("select name from characters where accountid = (select accountid from characters where name = ?)")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.isBeforeFirst()) {
                            while (rs.next()) {
                                usernames.add(rs.getString("name"));
                            }
                        } else {
                            player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
                            return;
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("characters: ");
                    usernames.forEach(s -> sb.append(s).append(", "));
                    sb.setLength(sb.length() - 2);
                    player.dropMessage(sb.toString());
                    sb.setLength(0);
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.dropMessage(5, "An error occurred");
                }
            } else {
                player.dropMessage(5, "You must specify a username");
            }
        } else if (command.equals("jail")) {
            if (args.length() >= 2) {
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target != null) {
                    String reason = args.concatFrom(1);
                    if (!reason.isEmpty()) {
                        JailManager.insertJail(target.getId(), player.getId(), reason);
                        target.changeMap(JailManager.getRandomField());
                        target.sendMessage(5, "You have been jailed by '{}'", player.getName());
                        client.getWorldServer().broadcastMessage(6, "'{}' has been jailed for '{}'", target.getName(), reason);
                    } else {
                        player.sendMessage(5, "You must provide a reason for your jail");
                    }
                } else {
                    player.sendMessage(5, "Unable to find player named '{}'", args.get(0));
                }
            } else if (args.length() == 1 && args.get(0).equalsIgnoreCase("logs")) {
                ArrayList<JailManager.JailLog> logs = JailManager.retrieveLogs();
                for (JailManager.JailLog log : logs) {
                    String tUsername = MapleCharacter.getNameById(log.playerId);
                    String aUsername = MapleCharacter.getNameById(log.accuser);
                    player.sendMessage("'{}' jailed '{}' for '{}' on {}", aUsername, tUsername, log.reason, DateFormat.getDateTimeInstance().format(log.timestamp));
                }
            } else {
                player.dropMessage(5, "Correct usage: !jail <username> <reason>");
            }
        } else if (command.equals("unjail")) {
            if (args.length() == 1) {
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target != null) {
                    JailManager.removeJail(target.getId());
                    if (JailManager.isJailField(target.getMapId())) {
                        target.changeMap(ServerConstants.HOME_MAP);
                    }
                    target.sendMessage(6, "You have been unjailed by '{}'", player.getName());
                    player.sendMessage(6, "Success!");
                } else {
                    player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                }
            }
        } else if (command.equals("search")) {
            if (args.length() > 1) {
                String type = args.get(0);
                String search = args.concatFrom(1).toLowerCase().trim();
                MapleData data;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz"));
                player.message("<<Type: " + type + " | Search: \"" + search + "\">>");

                BiPredicate<String, String> check = new BiPredicate<>() {
                    @Override
                    public boolean test(String query, String item) {
                        String[] keywords = query.split(" ");
                        int matches = 0;
                        for (String words : keywords) {
                            if (item.toLowerCase().contains(words)) {
                                matches++;
                            }
                        }
                        return matches == keywords.length;
                    }
                };

                if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) {
                    List<String> retNpcs = new ArrayList<>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                    for (MapleData npcIdData : data.getChildren()) {
                        int npcIdFromData = Integer.parseInt(npcIdData.getName());
                        String npcNameFromData = MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME");
                        npcPairList.add(new Pair<>(npcIdFromData, npcNameFromData));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (check.test(search, npcPair.getRight())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            player.message(singleRetNpc);
                        }
                        retNpcs.clear();
                        npcPairList.clear();
                    } else {
                        player.message("No NPC's Found");
                    }
                } else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) {
                    List<String> retMaps = new ArrayList<>();
                    data = dataProvider.getData("Map.img");
                    if (data == null) {
                        player.dropMessage("An error occurred while looking for map names");
                        return;
                    }
                    List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            int mapIdFromData = Integer.parseInt(mapIdData.getName());
                            String mapNameFromData = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                            mapPairList.add(new Pair<>(mapIdFromData, mapNameFromData));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (check.test(search, mapPair.getRight())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            player.message(singleRetMap);
                        }
                        retMaps.clear();
                        mapPairList.clear();
                    } else {
                        player.message("No Maps Found");
                    }
                } else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS") || type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) {
                    List<String> retMobs = new ArrayList<>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                    for (MapleData mobIdData : data.getChildren()) {
                        int mobIdFromData = Integer.parseInt(mobIdData.getName());
                        String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
                        mobPairList.add(new Pair<>(mobIdFromData, mobNameFromData));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (check.test(search, mobPair.getRight())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            player.message(singleRetMob);
                        }
                        retMobs.clear();
                        mobPairList.clear();
                    } else {
                        player.message("No Mob's Found");
                    }
                } else if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) {
                    List<String> retItems = new ArrayList<>();
                    for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (check.test(search, itemPair.getRight())) {
                            retItems.add(itemPair.getLeft() + " - " + itemPair.getRight());
                        }
                    }
                    if (retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            player.message(singleRetItem);
                        }
                        retItems.clear();
                    } else {
                        player.message("No Item's Found");
                    }
                }
            } else {
                player.message("Syntax: !search <type> <name> where type is map, use, etc, cash, equip or mob.");
            }
        } else if (command.equals("chattype")) {
            if (args.length() == 1) {
                Integer ordinal = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                ChatType[] types = ChatType.values();
                player.setChatType(types[ordinal]);
                player.dropMessage("Chat type set to '" + types[ordinal].name().toLowerCase() + "'");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Available chat types:\r\n");
                for (ChatType type : ChatType.values()) {
                    sb.append("\r\n").append(type.ordinal()).append(" - ").append(type.name().toLowerCase());
                }
                player.dropMessage(1, sb.toString());
                sb.setLength(0);
            }
        } else if (command.equals("ap", "sp")) {
            boolean ap = command.equals("ap");
            if (args.length() > 0) {
                Short amount = args.parseNumber(0, short.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                if (args.length() > 1) {
                    for (int i = 1; i < args.length(); i++) {
                        final String username = args.get(i);
                        MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (target != null) {
                            if (ap) {
                                target.setRemainingAp(amount);
                            } else {
                                target.setRemainingSp(amount);
                            }
                            target.updateSingleStat(ap ? MapleStat.AVAILABLEAP : MapleStat.AVAILABLESP, amount);
                            target.dropMessage("Your available " + command.getName().toUpperCase() + " has been updated to " + (ap ? target.getRemainingAp() : target.getRemainingSp()));
                        }
                    }
                    player.dropMessage("Done!");
                } else {
                    if (ap) {
                        player.setRemainingAp(amount);
                    } else {
                        player.setRemainingSp(amount);
                    }
                    player.updateSingleStat(ap ? MapleStat.AVAILABLEAP : MapleStat.AVAILABLESP, amount);
                    player.dropMessage("Done!");
                }
            }
        } else if (command.equals("buff")) {
            int[] skills = {1001003, 2001002, 1101006, 1101007, 1301007, 2201001, 2121004, 2111005, 2311003, 1121002, 4211005, 3121002, 1121000, 2311003, 1101004, 1101006, 4101004, 4111001, 2111005, 1111002, 2321005, 3201002, 4101003, 4201002, 5101006, 1321010, 1121002, 1120003};
            MapleCharacter target = player;
            if (args.length() == 1) {
                target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target == null) {
                    player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                    return;
                }
            }
            for (int skill : skills) {
                Skill s = SkillFactory.getSkill(skill);
                if (s != null) {
                    s.getEffect(s.getMaxLevel()).applyTo(target);
                }
            }
        } else if (command.equals("fame")) {
            if (args.length() > 0) {
                Integer amount = args.parseNumber(0, int.class);
                if (amount == null) {
                    player.sendMessage(5, args.getFirstError());
                } else if (amount < 0 || amount > 32767) {
                    player.sendMessage(5, "You cannot give that much fame");
                } else if (args.length() > 1) {
                    for (int i = 1; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter target = client.getChannelServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (target != null) {
                            target.setFame(amount);
                            target.updateSingleStat(MapleStat.FAME, amount);
                        } else {
                            player.sendMessage(5, "Unable to find any player named '{}'", username);
                        }
                    }
                } else {
                    player.setFame(amount);
                    player.updateSingleStat(MapleStat.FAME, amount);
                }
            } else {
                player.sendMessage(5, "syntax: !fame <amount> [usernames...]");
            }
        } else if (command.equals("mesos")) {
            if (args.length() > 0) {
                Integer mesos = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(String.format("%s is not a number", args.get(0)));
                    return;
                }
                if (args.length() > 1) {
                    for (int i = 1; i < args.length(); i++) {
                        String username = args.get(i);
                        MapleCharacter chr = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (chr != null) {
                            chr.gainMeso(mesos, true);
                        } else {
                            player.dropMessage(String.format("Unable to find any player named '%s'", username));
                        }
                    }
                } else {
                    player.gainMeso(mesos, true);
                }
            } else {
                player.gainMeso(Integer.MAX_VALUE - player.getMeso(), true);
            }
        } else if (command.equals("gender")) {
            if (args.length() == 2) {
                MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
                byte gender;
                switch (args.get(1)) {
                    case "male":
                        gender = 0;
                        break;
                    case "female":
                        gender = 1;
                        break;
                    case "uni":
                        gender = 2;
                        break;
                    default:
                        player.sendMessage("Invalid gender type '{}'", args.get(1));
                        return;
                }
                if (target != null) {
                    target.announce(MaplePacketCreator.updateGender(gender));
                    target.setGender(gender);
                    target.sendMessage("Your gender has changed to {}", args.get(1));
                    player.dropMessage("Success!");
                } else {
                    player.sendMessage("Unable to find any player named '{}'", args.get(0));
                }
            } else {
                player.dropMessage(5, "Syntax: !gender <username> [male/female/uni]");
            }
        } else if (command.equals("setall")) {
            if (args.length() > 0) {
                Integer stat = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                MapleCharacter target = player;
                if (args.length() == 2) {
                    target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(1)));
                    if (target == null) {
                        player.dropMessage(String.format("Unable to find any player named '%s'", args.get(1)));
                        return;
                    }
                }
                if (stat >= 0 && stat <= Short.MAX_VALUE) {
                    target.setStr(stat);
                    target.setDex(stat);
                    target.setInt(stat);
                    target.setLuk(stat);
                    target.updateSingleStat(MapleStat.STR, stat);
                    target.updateSingleStat(MapleStat.DEX, stat);
                    target.updateSingleStat(MapleStat.INT, stat);
                    target.updateSingleStat(MapleStat.LUK, stat);
                    if (target.getId() != player.getId()) {
                        player.dropMessage(String.format("Changed '%s' stats to %d", target.getName(), stat));
                    }
                } else {
                    player.dropMessage("You can't set your stats to that number");
                }
            } else {
                player.dropMessage(5, "Syntax: !setall <number> [username]");
            }
        }
    }
}