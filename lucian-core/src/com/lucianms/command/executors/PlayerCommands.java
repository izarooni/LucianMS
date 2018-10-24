package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleStat;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.CommandWorker;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.RockPaperScissorsEvent;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.features.PlayerBattle;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.SavedLocationType;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

import java.util.*;

/**
 * @author izarooni, lucasdieswagger
 */
public class PlayerCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();

        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.PlayerCommands);
        if (spamTracker.testFor(1300) && spamTracker.getTriggers() > 3) {
            player.sendMessage(5, "You are doing this too fast");
            return;
        }
        spamTracker.record();
        MapleChannel ch = client.getChannelServer();

        if (command.equals("help", "commands")) {
            boolean npc = args.length() == 1 && args.get(0).equals("npc");
            ArrayList<String> commands = new ArrayList<>();
            commands.add("@help - to see what commands there are");
            commands.add("@commands - another way to see the commands");
            commands.add("@rates - show the server rates");
            commands.add("@joinevent - join the GM event");
            commands.add("@leaveevent - leave the GM event");
            commands.add("@jautoevent - join the auto event");
            commands.add("@lautoevent - leave the auto event");
            commands.add("@dispose - Dispose yourself (if you can't interact with npcs, etc) ");
            commands.add("@achievements - Shows your current achievements");
            commands.add("@home - go to the home grounds");
            commands.add("@online - Show whoever is online");
            commands.add("@go <town|list> - warps you to a town or shows you a list of warpable towns");
            commands.add("@style - open the styling npc");
            commands.add("@kin - alternative to opening the syle npc");
            commands.add("@callgm <reason> - report a player, insert your own name if it is for another reason.");
            commands.add("@report <bug> - report a bug, give as much detail as possible.");
            commands.add("@rps - Start a game of rock paper scissors vs a bot");
            commands.add("@arcade - Warp to the arcade map");
            commands.add("@update - display latest WZ revision");
            commands.add("@reset<str/dex/int/luk/stats> - Reset assigned AP");
            commands.add("@<str/dex/int/luk> - Assign any available AP to a specified stat");
            commands.add("@checkme - Check your stats");
            commands.add("@spy <player> - Check another player's stats");
            commands.add("@fixexp - Reset EXP");
            commands.add("@serverinfo - Displays server information");
            commands.add("@shenron - Warp to the Shenron summoning map");
            commands.add("@quests - List your quests currently in-progress");
            commands.add("@afk <ign> - Check if someone is AFK");
            commands.add("@uptime - Display how long the server has been live");
            commands.add("@time - Display the current server time");
            commands.add("@house - Display the house manager NPC");
            commands.add("@jobs - Display a list of job modifications");
//            commands.add("@rebirth - Reset your player level to earn more AP");
            commands.sort(String::compareTo);
            if (npc) {
                StringBuilder sb = new StringBuilder();
                commands.forEach(s -> sb.append("\r\n").append(s));
                client.announce(MaplePacketCreator.getNPCTalk(2007, (byte) 0, sb.toString(), "00 00", (byte) 0));
            } else {
                commands.forEach(player::dropMessage);
                player.dropMessage("If you'd like to view this list in an NPC window, use the command < @help npc >");
            }
            commands.clear();
        } else if (command.equals("house")) {
            NPCScriptManager.start(client, 2007, "f_house_manager");
        } else if (command.equals("time")) {
            player.sendMessage("Server time is: {}", Calendar.getInstance().getTime().toString());
        } else if (command.equals("uptime")) {
            player.sendMessage("The server has been online for {}", StringUtil.getTimeElapse(System.currentTimeMillis() - Server.Uptime));
        } else if (command.equals("checkme", "spy")) {
            MapleCharacter target = player;
            if (command.equals("spy")) {
                if (args.length() == 1) {
                    target = ch.getPlayerStorage().getPlayerByName(args.get(0));
                    if (target == null) {
                        player.sendMessage("Unable to find any player named '{}'", args.get(0));
                        return;
                    }
                } else {
                    player.sendMessage("Invalid argument count; usage: @{} <username>", command.getName());
                    return;
                }
            }
            player.sendMessage("================ '{}''s Stats ================", target.getName());
            player.sendMessage("EXP {}x, MESO {}x, DROP {}x", player.getExpRate(), player.getMesoRate(), player.getDropRate());
            //   player.sendMessage("Rebirths: {}", player.getRebirths());
            player.sendMessage("Mesos: {}", StringUtil.formatNumber(target.getMeso()));
            player.sendMessage("Ability Power: {}", StringUtil.formatNumber(target.getRemainingAp()));
            player.sendMessage("Hair / Face: {} / {}", target.getHair(), target.getFace());
            player.sendMessage("Crystals: {}", target.getItemQuantity(ServerConstants.CURRENCY, false));
            Occupation occupation = target.getOccupation();
            player.sendMessage("Occupation: {}", (occupation == null) ? "N/A" : occupation.getType().name());
            player.sendMessage("Fishing Points: {}", StringUtil.formatNumber(target.getFishingPoints()));
            player.sendMessage("Event Points" + StringUtil.formatNumber(target.getEventPoints()));
            player.sendMessage("Donor Points: {}", StringUtil.formatNumber(client.getDonationPoints()));
            player.sendMessage("Vote points: {}", StringUtil.formatNumber(client.getVotePoints()));
        } else if (command.matches("^reset(stats|str|dex|int|luk)$")) {
            String statName = command.getName().substring(5);

            List<Pair<MapleStat, Integer>> statChange = new ArrayList<>(4);
            int nStat = 0;
            switch (statName) {
                case "stats":
                    nStat += player.getStr() - 4;
                    nStat += player.getDex() - 4;
                    nStat += player.getInt() - 4;
                    nStat += player.getLuk() - 4;

                    player.setStr(4);
                    player.setDex(4);
                    player.setInt(4);
                    player.setLuk(4);
                    statChange.add(new Pair<>(MapleStat.STR, 4));
                    statChange.add(new Pair<>(MapleStat.DEX, 4));
                    statChange.add(new Pair<>(MapleStat.INT, 4));
                    statChange.add(new Pair<>(MapleStat.LUK, 4));
                    break;
                case "str":
                    nStat = player.getStr() - 4;
                    player.setStr(4);
                    statChange.add(new Pair<>(MapleStat.STR, 4));
                    break;
                case "dex":
                    nStat = player.getDex() - 4;
                    player.setDex(4);
                    statChange.add(new Pair<>(MapleStat.DEX, 4));
                    break;
                case "int":
                    nStat = player.getInt() - 4;
                    player.setInt(4);
                    statChange.add(new Pair<>(MapleStat.INT, 4));
                    break;
                case "luk":
                    nStat = player.getLuk() - 4;
                    player.setLuk(4);
                    statChange.add(new Pair<>(MapleStat.LUK, 4));
                    break;
            }
            player.setRemainingAp(player.getRemainingAp() + nStat);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
            player.announce(MaplePacketCreator.updatePlayerStats(statChange, player));
            player.dropMessage(6, "Done!");
        } else if (command.equals("afk", "away")) {
            if (args.length() >= 1) {
                MapleCharacter target = ch.getPlayerStorage().getPlayerByName(args.get(0));
                if (target != null) {
                    player.dropMessage(String.format("%s is currently %s", target.getName(), (target.getClient().getSession().isActive() ? "AFK" : "not AFK")));
                } else {
                    player.dropMessage("The player you tried to check is not online, or does not exist.");
                }
            } else {
                player.dropMessage("Please make sure to specify a username.");
            }
        } else if (command.matches("^(str|dex|int|luk)$")) {
            if (args.length() == 1) {
                Integer nStat = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    player.dropMessage(5, error);
                    return;
                }
                if (nStat == 0) {
                    player.dropMessage("You need to enter a number less than, or larger than 0");
                    return;
                } else if (nStat > 0 && nStat > player.getRemainingAp()) {
                    player.dropMessage("Make sure you have enough AP to distribute");
                    return;
                }
                switch (command.getName()) {
                    case "str":
                        if (nStat > 0 && player.getStr() + nStat > Short.MAX_VALUE) {
                            player.dropMessage("You can't assign that much AP to the STR stat");
                        } else if (nStat < 0 && player.getStr() + nStat < 0) {
                            player.dropMessage("You can't take away what you don't have!");
                        } else {
                            player.setStr(player.getStr() + nStat);
                            player.setRemainingAp(player.getRemainingAp() - nStat);
                            player.updateSingleStat(MapleStat.STR, player.getStr());
                            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());

                        }
                        break;
                    case "dex":
                        if (nStat > 0 && player.getDex() + nStat > Short.MAX_VALUE) {
                            player.dropMessage("You can't assign that much AP to the DEX stat");
                        } else if (nStat < 0 && player.getDex() + nStat < 0) {
                            player.dropMessage("You can't take away what you don't have!");
                        } else {
                            player.setDex(player.getDex() + nStat);
                            player.setRemainingAp(player.getRemainingAp() - nStat);
                            player.updateSingleStat(MapleStat.DEX, player.getDex());
                            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());

                        }
                        break;
                    case "int":
                        if (nStat > 0 && player.getInt() + nStat > Short.MAX_VALUE) {
                            player.dropMessage("You can't assign that much AP to the INT stat");
                        } else if (nStat < 0 && player.getInt() + nStat < 0) {
                            player.dropMessage("You can't take away what you don't have!");
                        } else {
                            player.setInt(player.getInt() + nStat);
                            player.setRemainingAp(player.getRemainingAp() - nStat);
                            player.updateSingleStat(MapleStat.INT, player.getInt());
                            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());

                        }
                        break;
                    case "luk":
                        if (nStat > 0 && player.getLuk() + nStat > Short.MAX_VALUE) {
                            player.dropMessage("You can't assign that much AP to the LUK stat");
                        } else if (nStat < 0 && player.getLuk() + nStat < 0) {
                            player.dropMessage("You can't take away what you don't have!");
                        } else {
                            player.setLuk(player.getLuk() + nStat);
                            player.setRemainingAp(player.getRemainingAp() - nStat);
                            player.updateSingleStat(MapleStat.LUK, player.getLuk());
                            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                        }
                        break;
                }
            } else {
                player.dropMessage(5, String.format("Syntax: @%s <amount>", command.getName()));
            }
        } else if (command.equals("rates")) {
            player.dropMessage(6, "EXP rate: " + player.getExpRate());
            player.dropMessage(6, "Drop rate: " + player.getDropRate());
            player.dropMessage(6, "Meso rate: " + player.getMesoRate());
        } else if (command.equals("jobs")) {
            NPCScriptManager.start(client, 9900000, null);
        } else if (command.equals("joinevent", "leaveevent")) {
            boolean join = command.equals("joinevent");
            ManualPlayerEvent playerEvent = client.getWorldServer().getPlayerEvent();
            if (playerEvent != null) {
                if (!playerEvent.isOpen()) {
                    player.sendMessage(5, "The event is no longer open");
                    return;
                }
                if (join) {
                    if (player.getMap() != playerEvent.getMap() && !playerEvent.participants.containsKey(player.getId())) {
                        ManualPlayerEvent.Participant p = new ManualPlayerEvent.Participant(player.getId(), player.getMapId());
                        playerEvent.participants.put(player.getId(), p);
                        player.changeMap(playerEvent.getMap(), playerEvent.getSpawnPoint());
                    } else {
                        player.sendMessage(5, "You are already in the event!");
                    }
                } else {
                    ManualPlayerEvent.Participant p = playerEvent.participants.get(player.getId());
                    if (p != null) {
                        player.changeMap(p.returnMapId);
                    } else {
                        player.sendMessage(5, "You are not in an event");
                    }
                }
            } else {
                player.sendMessage(5, "There is no event going on right now");
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
//        } else if (command.equals("rebirth")) {
//            if (player.getLevel() >= player.getMaxLevel()) {
//                player.doRebirth();
//                player.sendMessage("You now have {} rebirths!", player.getRebirths());
//            } else {
//                player.sendMessage("You must be at least level {} before you can rebirth", player.getMaxLevel());
//            }
        } else if (command.equals("dispose")) {
            NPCScriptManager.dispose(client);
            player.announce(MaplePacketCreator.enableActions());
            player.getMap().getMonsters().stream().filter(m -> !m.isAlive()).forEach(m -> player.getMap().killMonster(m, null, false));
            player.dropMessage(6, "Disposed!");
        } else if (command.equals("achievements")) {
            NPCScriptManager.start(client, 2007, "f_achievements");
        } else if (command.equals("home")) {
            player.saveLocation(SavedLocationType.FREE_MARKET.name());
            player.changeMap(ServerConstants.HOME_MAP);
        } else if (command.equals("serverinfo")) {
            // why is this a command, seriously?
            player.dropMessage("Version: 83");
            player.dropMessage("Rates: 10x | 10x | 2x");
            player.dropMessage("Owner: Venem");
            player.dropMessage("Developers: izarooni");
            player.dropMessage("Staff: Kill | Evan | Joey | Jackie | Luckedy | Bryan");
            player.dropMessage("Home Command: @home or @go fm");
            player.dropMessage("Main Website: http://lucianms.com");
            player.dropMessage("Voting resets every 24th hours!");
            player.dropMessage("Have Fun and consider to donate for more customs!");
        } else if (command.equals("update")) {
            player.dropMessage("Last Server WZ revision: October 13, 2018");
            player.dropMessage("New customs added every 2nd month!");
        } else if (command.equals("online")) {
            for (MapleChannel channel : client.getWorldServer().getChannels()) {
                StringBuilder sb = new StringBuilder();
                for (MapleCharacter players : channel.getPlayerStorage().getAllPlayers()) {
                    if (!players.isGM()) {
                        sb.append(players.getName()).append(" ");
                    }
                }
                player.dropMessage(6, String.format("Channel(%d): %s", channel.getId(), sb.toString()));
            }
        } else if (command.equals("go")) {
            WeakHashMap<String, Integer> maps = new WeakHashMap<>();
            // @formatter:off
            maps.put("quay",       541000000);
            maps.put("magatia",    261000000);
            maps.put("elnath",     211000000);
            maps.put("shenron",    908);
            maps.put("nlc",        600000000);
            maps.put("fm",         910000000);
            maps.put("henesys",    100000000);
            maps.put("maya",       100000001);
            maps.put("ellinia",    101000000);
            maps.put("perion",     102000000);
            maps.put("kerning",    103000000);
            maps.put("lith",       104000000);
            maps.put("harbor",     104000000);
            maps.put("florina",    110000000);
            maps.put("nautilus",   120000000);
            maps.put("ereve",      130000000);
            maps.put("rien",       140000000);
            maps.put("orbis",      200000000);
            maps.put("ludi",       220000000);
            maps.put("aqua",       230000000);
            maps.put("leafre",     240000000);
            maps.put("mulung",     250000000);
            maps.put("ariant",     260000000);
            maps.put("timetemple", 270000000);
            maps.put("ellin",      300000000);
            maps.put("home",       910000000);
            maps.put("arcade",     978);
            // @formatter:on
            if (args.length() == 1) {
                String name = args.get(0).toLowerCase(Locale.ROOT);
                if (maps.containsKey(name)) {
                    MapleMap map = ch.getMap(maps.get(name));
                    if (map != null) {
                        if (player.getJQController() != null) {
                            player.setJQController(null);
                        }
                        player.changeMap(map);
                    } else {
                        player.dropMessage(5, "Unable to warp to map " + name);
                    }
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String s : maps.keySet()) {
                sb.append(s).append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.dropMessage(5, "These are the current maps available for you to warp to");
            player.dropMessage(5, sb.toString());
            maps.clear();
        } else if (command.equals("callgm")) {
            if (args.length() > 0) {
                String message = args.concatFrom(0);
                if (!message.isEmpty()) {
                    client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, String.format("[GM_CALL] %s: %s", player.getName(), message)));
                    player.dropMessage(6, "Help message sent");
                } else {
                    player.dropMessage(5, "You must specify a message");
                }
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else if (command.equals("style", "styler", "stylist")) {
            NPCScriptManager.start(player.getClient(), 9900001);
        } else if (command.equals("report")) {
            if (args.length() > 1) {
                String username = args.get(0);
                String message = args.concatFrom(1);
                MapleCharacter target = client.getWorldServer().getPlayerStorage().getPlayerByName(username);
                if (target != null) {
                    client.getWorldServer().broadcastGMPacket(MaplePacketCreator.serverNotice(6, String.format("[REPORT] %s : (%s) %s", player.getName(), username, message)));
                } else {
                    player.dropMessage(5, String.format("Could not find any player named '%s'", username));
                }
            } else {
                player.dropMessage(5, "You must specify a username and message");
            }
        } else if (command.equals("pvp")) {
            PlayerBattle battle = player.getPlayerBattle();
            if (battle != null) {
                if (battle.getLastAttack() > 5) {
                    battle.unregisterPlayer(player);
                    player.sendMessage(6, "You are no longer in PvP mode");
                } else {
                    player.sendMessage(6, "You cannot exit PvP while in combat");
                }
            } else {
                new PlayerBattle().registerPlayer(player);
                player.sendMessage(6, "You are now PvPing");
            }
        } else if (command.equals("rps")) {
            RockPaperScissorsEvent.startGame(player);
            player.dropMessage(6, "Let's play some rock paper scissors!");
        } else if (command.equals("shenron")) {
            player.changeMap(908);
        } else if (command.equals("quests")) {
            NPCScriptManager.start(client, 2007, "f_quests");
        } else if (command.equals("fixexp", "expfix")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, 0);
        } else {
            player.dropMessage("Use @help for a list of our available commands");
        }
    }
}
