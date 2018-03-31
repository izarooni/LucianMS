package command;

import client.*;
import client.Relationship.Status;
import client.meta.Occupation;
import constants.ServerConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.handlers.RockPaperScissorsHandler;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.events.custom.GenericEvent;
import server.events.custom.ManualPlayerEvent;
import server.events.custom.auto.GAutoEvent;
import server.events.custom.auto.GAutoEventManager;
import server.events.pvp.PlayerBattle;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

import java.util.*;

/**
 * @author izarooni, lucasdieswagger
 */
public class PlayerCommands {

    // TODO: Not all command will be seen due to it overflowing the entire text screen!
    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {
        MapleCharacter player = client.getPlayer();

        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.PlayerCommands);
        if (!spamTracker.testFor(1300) && spamTracker.getTriggers() > 3) {
            player.sendMessage(5, "You are doing this too fast");
            return;
        }
        spamTracker.record();

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
            commands.add("@achievements - Shows your current achievements");
            commands.add("@home - go to the home grounds");
            commands.add("@online - Show whoever is online");
            commands.add("@go <town|list> - warps you to a town or shows you a list of warpable towns");
            commands.add("@style - open the styling npc");
            commands.add("@kin - alternative to opening the syle npc");
            commands.add("@callgm <player> <reason> - report a player, insert your own name if it is for another reason.");
            commands.add("@report <bug> - report a bug, give as much detail as possible.");
            commands.add("@rps - Start a game of rock paper scissors vs a bot");
            commands.add("@summer - Warp to the summer map");
            commands.add("@arcade - Warp to the arcade map");
            commands.add("@reset<str/dex/int/luk/stats> - Reset assigned AP");
            commands.add("@<str/dex/int/luk> - Assign any available AP to a specified stat");
            commands.add("@checkme - Check your player's stats");
            commands.add("@spy <player> - See the stats of a player");
            commands.add("@fixexp - Reset EXP");
            commands.add("@shenron - Warp to the Shenron summoning map");
            commands.add("@quests - List your quests currently in-progress");
            commands.add("@uptime - Display how long the server has been live");
            commands.add("@time - Display the current server time");
            Collections.sort(commands);
            commands.forEach(player::dropMessage);
            commands.clear();
        } else if (command.equals("time")) {
            player.sendMessage("Server time is: {}", Calendar.getInstance().getTime().toString());
        } else if (command.equals("uptime")) {
            player.sendMessage("The server has been online for {}", StringUtil.getTimeElapse(System.currentTimeMillis() - Server.Uptime));
        } else if (command.equals("checkme", "spy")) {
            MapleCharacter target = player;
            if (command.equals("spy")) {
                if (args.length() == 1) {
                    target = ch.getPlayerStorage().getCharacterByName(args.get(0));
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
                    nStat += player.getStr();
                    nStat += player.getDex();
                    nStat += player.getInt();
                    nStat += player.getLuk();

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
                    nStat = player.getStr();
                    player.setStr(4);
                    statChange.add(new Pair<>(MapleStat.STR, 4));
                    break;
                case "dex":
                    nStat = player.getDex();
                    player.setDex(4);
                    statChange.add(new Pair<>(MapleStat.DEX, 4));
                    break;
                case "int":
                    nStat = player.getInt();
                    player.setInt(4);
                    statChange.add(new Pair<>(MapleStat.INT, 4));
                    break;
                case "luk":
                    nStat = player.getLuk();
                    player.setLuk(4);
                    statChange.add(new Pair<>(MapleStat.LUK, 4));
                    break;
            }
            player.setRemainingAp(player.getRemainingAp() + nStat);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
            player.announce(MaplePacketCreator.updatePlayerStats(statChange, player));
            player.dropMessage(6, "Done!");
        } else if (command.matches("^(str|dex|int|luk)$")) {
            if (args.length() == 1) {
                Long var_nStat = args.parseNumber(0);
                if (var_nStat == null) {
                    player.dropMessage(String.format("%s is not a number", args.get(0)));
                    return;
                }
                int nStat = var_nStat.intValue();
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
        } else if (command.equals("job")) {
            NPCScriptManager.start(client, 9201095, null);
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
        } else if (command.equals("dispose")) {
            NPCScriptManager.dispose(client);
            player.announce(MaplePacketCreator.enableActions());
            player.getMap().getMonsters().stream().filter(m -> !m.isAlive()).forEach(m -> player.getMap().killMonster(m, null, false));
            player.dropMessage(6, "Disposed!");
        } else if (command.equals("achievements")) {
            NPCScriptManager.start(client, 2007, "f_achievements");
        } else if (command.equals("home")) {
            player.changeMap(ServerConstants.HOME_MAP);
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
            // @formatter:off
            maps.put("fm",         809);
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
            // @formatter:on
            if (args.length() == 1) {
                String name = args.get(0).toLowerCase(Locale.ROOT);
                if (maps.containsKey(name)) {
                    MapleMap map = ch.getMapFactory().getMap(maps.get(name));
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
            NPCScriptManager.start(player.getClient(), 9900001);
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
        } else if (command.equals("marry")) {
            if (args.length() >= 1) {
                Relationship playerRelation = player.getRelationship();
                if (!(args.get(0).equals("deny"))) {
                    MapleCharacter target = ch.getPlayerStorage().getCharacterByName(args.get(0));
                    if (target != null) {
                        Relationship targetRelation = target.getRelationship();
                        if (!(target.getRelationship().getStatus() == Status.Engaged || target.getRelationship().getStatus() == Status.Married)) {
                            targetRelation.setBrideId(target.getId());
                            targetRelation.setGroomId(target.getId());
                            targetRelation.setStatus(Status.Engaged);

                            playerRelation.setBrideId(target.getId());
                            playerRelation.setGroomId(target.getId());
                            playerRelation.setStatus(Status.Engaged);
                            player.dropMessage(6, String.format("You requested to marry %s", target.getName()));
                            target.dropMessage(6, String.format("%s has requested to marry you, type @marry %s to accept it or @marry deny to deny it.", player.getName(), player.getName()));
                        } else {
                            if (targetRelation.getGroomId() == player.getId() && targetRelation.getStatus() != Status.Married) {
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
                    if (player.getRelationship().getStatus() == Status.Engaged) {
                        player.getRelationship().setStatus(Status.Single);
                        MapleCharacter target = ch.getPlayerStorage().getCharacterById(player.getRelationship().getBrideId());
                        if (target != null) {
                            target.getRelationship().setStatus(Status.Single);
                            target.dropMessage(5, String.format("%s has denied your marriage request", player.getName()));
                        }
                        player.dropMessage(6, "You have denied the marriage request.");
                    }
                }
            }
        } else if (command.equals("rps")) {
            RockPaperScissorsHandler.startGame(player);
            player.dropMessage(6, "Let's play some rock paper scissors!");
        } else if (command.equals("arcade")) {
            player.changeMap(978);
        } else if (command.equals("shenron")) {
            player.changeMap(908);
        } else if (command.equals("quests")) {
            NPCScriptManager.start(client, 2007, "f_quests");
        } else if (command.equals("crystal")) {
            if (player.getMeso() >= 500000000) {
                if (MapleInventoryManipulator.checkSpace(client, ServerConstants.CURRENCY, 1, "")) {
                    MapleInventoryManipulator.addById(client, ServerConstants.CURRENCY, (short) 1);
                    player.gainMeso(-500000000, true);
                    player.dropMessage("You now have " + player.getItemQuantity(ServerConstants.CURRENCY, false) + " crystals");
                } else {
                    player.dropMessage("You don't have enough inventory space for this exchange");
                }
            } else {
                player.dropMessage("You need 500M mesos to exchange for a crystal");
            }
        } else if (command.equals("fixexp", "expfix")) {
            player.setExp(0);
            player.updateSingleStat(MapleStat.EXP, 0);
        } else {
            player.dropMessage("Use @help for a list of our available commands");
        }
    }
}
