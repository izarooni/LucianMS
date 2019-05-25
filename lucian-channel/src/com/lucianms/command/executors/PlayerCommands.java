package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleStat;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.RockPaperScissorsEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.features.PlayerBattle;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.SavedLocationType;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author izarooni, lucasdieswagger
 */
public class PlayerCommands extends CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerCommands.class);

    private static void CollectLeaderboard(Connection con, List<String> usernames, String query) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usernames.add(rs.getString("name"));
                }
            }
        }
    }

    public PlayerCommands() {
        addCommand("help", this::Help);
        addCommand("commands", this::Help);

        addCommand("joinevent", this::ParticipateEvent);
        addCommand("leaveevent", this::ParticipateEvent);

        addCommand("jautoevent", this::ParticipateAutoEvent);
        addCommand("lautoevent", this::ParticipateAutoEvent);

        addCommand("dispose", this::Dispose);
        addCommand("achievements", this::Achievements);
        addCommand("home", this::Home);
        addCommand("online", this::Online);

        addCommand("go", this::Go);
        addCommand("shenron", this::Go);
        addCommand("arcade", this::Go);

        addCommand("style", this::Style);
        addCommand("callgm", this::CallGM);
        addCommand("report", this::Report);
        addCommand("rps", this::RPS);

        addCommand("resetstats", this::ResetStats);
        addCommand("resetstr", this::ResetStats);
        addCommand("resetdex", this::ResetStats);
        addCommand("resetint", this::ResetStats);
        addCommand("resetluk", this::ResetStats);

        addCommand("str", this::SetStat);
        addCommand("dex", this::SetStat);
        addCommand("int", this::SetStat);
        addCommand("luk", this::SetStat);

        addCommand("checkme", this::CheckPlayer);
        addCommand("check", this::CheckPlayer);
        addCommand("spy", this::CheckPlayer);

        addCommand("fixexp", this::FixExp);
        addCommand("quests", this::Quests);
        addCommand("afk", this::SetAFK);
        addCommand("uptime", this::ServerUptime);
        addCommand("time", this::ServerTime);
        addCommand("house", this::House);
        addCommand("jobs", this::Jobs);
        addCommand("chalktalk", this::ChalkTalk);
        addCommand("rebirth", this::Rebirth);
        addCommand("pvp", this::InitPlayerBattle);
        addCommand("bosshp", this::BossHP);
        addCommand("ranks", this::Ranks);

        addCommand("tradeep", this::TradePoints);
        addCommand("tradevp", this::TradePoints);
        addCommand("tradejq", this::TradePoints);
    }

    private void TradePoints(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() != 2) {
            player.sendMessage("Usage: !{} <username> <amount>", cmd.getName());
        }
        String username = args.get(0);
        MapleClient client = player.getClient();
        MapleCharacter target = client.getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
        if (target == null) {
            player.sendMessage("Unable to find any player named '{}'", username);
            return;
        }
        Integer amount = args.parseNumber(1, int.class);
        if (amount == null) {
            player.sendMessage(args.getFirstError());
            return;
        } else if (amount < 1) {
            player.sendMessage("You must trade at least 1 point of this type");
            return;
        }
        String type = cmd.getName().substring(5);
        switch (type) {
            default:
                player.sendMessage("'{}' is either not a valid points type or is not available for trading", type);
                break;
            case "vp":
                if (amount > client.getVotePoints()) {
                    player.sendMessage("You do not have that many vote points");
                } else {
                    target.getClient().addVotePoints(amount);
                    client.addVotePoints(-amount);
                    player.sendMessage("You have gave {} {} vote points", target.getName(), amount);
                    target.sendMessage("{} has given you {} vote points", player.getName(), amount);
                }
                break;
            case "ep":
                if (amount > target.getEventPoints()) {
                    player.sendMessage("You do not have that many event points");
                } else {
                    target.setEventPoints(target.getEventPoints() + amount);
                    player.setEventPoints(player.getEventPoints() - amount);
                    client.addVotePoints(-amount);
                    player.sendMessage("You have gave {} {} event points", target.getName(), amount);
                    target.sendMessage("{} has given you {} event points", player.getName(), amount);
                }
                break;
            case "jq":
                if (amount > target.getJumpQuestPoints()) {
                    player.sendMessage("You do not have that many jump quest points");
                } else {
                    target.setJumpQuestPoints(target.getJumpQuestPoints() + amount);
                    player.setJumpQuestPoints(player.getJumpQuestPoints() - amount);
                    client.addVotePoints(-amount);
                    player.sendMessage("You have gave {} {} jump quest points", target.getName(), amount);
                    target.sendMessage("{} has given you {} jump quest points", player.getName(), amount);
                }
                break;
        }
    }

    private void Ranks(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() != 1) {
            player.sendMessage("You must specify a leaderboard type. Currently, the available types are:");
            player.sendMessage("rebirths, coins, event, jq");
            return;
        }
        ArrayList<String> usernames = new ArrayList<>(10);
        String rankingType = args.get(0);
        try (Connection con = player.getClient().getWorldServer().getConnection()) {
            switch (rankingType) {
                default:
                    player.sendMessage("'{}' is not a valid leaderboard", rankingType);
                    break;
                case "rebirths":
                    CollectLeaderboard(con, usernames, "select name from characters where gm = 0 order by reborns desc");
                    break;
                case "coins":
                    CollectLeaderboard(con, usernames, "select c.name, i.characterid, sum(i.quantity) as total from inventoryitems i inner join characters c where c.id = i.characterid and itemid = " + ServerConstants.CURRENCY + " and gm = 0 group by characterid order by total desc");
                    break;
                case "event":
                    CollectLeaderboard(con, usernames, "select name from characters where gm = 0 order by eventpoints desc");
                    break;
                case "jq":
                    CollectLeaderboard(con, usernames, "select name from characters where gm = 0 order by jumpquestpoints desc");
                    break;
            }
        } catch (SQLException e) {
            player.sendMessage("Rankings for {} are currently unavailable", rankingType);
            LOGGER.error("Failed to retrieve rankings for {}", rankingType, e);
            return;
        }
        if (usernames.isEmpty()) {
            player.sendMessage("This leaderboard is currently empty");
        } else {
            MaplePacketWriter w = new MaplePacketWriter();
            w.writeShort(SendOpcode.GUILD_OPERATION.getValue());
            w.write(73);
            w.writeInt(9040008);
            w.writeInt(usernames.size());
            for (String s : usernames) {
                w.writeMapleString(s);
                w.writeInt(0);
                w.writeInt(0);
                w.writeInt(0);
                w.writeInt(0);
                w.writeInt(0);
            }
            player.announce(w.getPacket());
        }
    }

    private void BossHP(MapleCharacter player, Command cmd, CommandArgs args) {
        for (MapleMonster monster : player.getMap().getMonsters()) {
            if (monster.isBoss()) {
                player.sendMessage(5, "Name: '{}', Level: {}, HP: {} / {}",
                        monster.getName(), monster.getLevel(),
                        StringUtil.formatNumber(monster.getHp()), StringUtil.formatNumber(monster.getMaxHp()));
            }
        }
    }

    private void InitPlayerBattle(MapleCharacter player, Command cmd, CommandArgs args) {
        Optional<GenericEvent> opt = player.getGenericEvents().stream().filter(g -> g instanceof PlayerBattle).findFirst();
        if (opt.isPresent()) {
            PlayerBattle battle = (PlayerBattle) opt.get();
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
    }

    private void Rebirth(MapleCharacter player, Command cmd, CommandArgs args) {
        if (player.getLevel() >= player.getMaxLevel()) {
            player.doRebirth();
            player.sendMessage("You now have {} rebirths", player.getRebirths());
        } else {
            player.sendMessage("You must be at least level {} before you can rebirth", player.getMaxLevel());
        }
    }

    private void ChalkTalk(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            String content = args.concatFrom(0);
            player.setChalkboard(content);
            player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, false));
        } else {
            player.setChalkboard(null);
            player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, true));
        }
    }

    private void Jobs(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 9900000, null);
    }

    private void House(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 2007, "f_house_manager");
    }

    private void ServerTime(MapleCharacter player, Command cmd, CommandArgs args) {
        player.sendMessage("Server time is: {}", Calendar.getInstance().getTime().toString());
    }

    private void ServerUptime(MapleCharacter player, Command cmd, CommandArgs args) {
        player.sendMessage("The server has been online for {}", StringUtil.getTimeElapse(System.currentTimeMillis() - Server.Uptime));
    }

    private void SetAFK(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() >= 1) {
            MapleCharacter target = player.getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target != null) {
                player.dropMessage(String.format("%s is currently %s", target.getName(), (target.getClient().getSession().isActive() ? "AFK" : "not AFK")));
            } else {
                player.dropMessage("The player you tried to check is not online, or does not exist");
            }
        } else {
            player.dropMessage("You must specify a username");
        }
    }

    private void Quests(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 2007, "f_quests");
    }

    private void FixExp(MapleCharacter player, Command cmd, CommandArgs args) {
        player.setExp(0);
        player.updateSingleStat(MapleStat.EXP, 0);
    }

    private void CheckPlayer(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleCharacter target = player;
        if (cmd.equals("spy")) {
            if (args.length() == 1) {
                target = player.getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target == null) {
                    player.sendMessage("Unable to find any player named '{}'", args.get(0));
                    return;
                }
            } else {
                player.sendMessage("Invalid argument count; usage: @{} <username>", cmd.getName());
                return;
            }
        }
        player.sendMessage("================ '{}''s Stats ================", target.getName());
        player.sendMessage("Mesos: {}", StringUtil.formatNumber(target.getMeso()));
        player.sendMessage("Ability Points: {}", StringUtil.formatNumber(target.getRemainingAp()));
        player.sendMessage("Skill Points: {}", StringUtil.formatNumber(target.getRemainingSp()));
        player.sendMessage("Hair / Face: {} / {}", target.getHair(), target.getFace());
        player.sendMessage("EXP {}x, MESO {}x, DROP {}x", target.getExpRate(), target.getMesoRate(), target.getDropRate());
        if (player.isGM()) {
            player.sendMessage("GM Level {}, PID {}, OID {}", target.getGMLevel(), target.getId(), target.getObjectId());
        }
        player.sendMessage("========== Etc ==========");
        player.sendMessage("Currency: {}", target.getItemQuantity(ServerConstants.CURRENCY, false));
        Optional<Occupation> occupation = Optional.ofNullable(target.getOccupation());
        player.sendMessage("Rebirths: {}", target.getRebirths());
        player.sendMessage("Occupation: {} Lv.{}", occupation.map(o -> o.getType().name()).orElse("N/A"), occupation.map(Occupation::getLevel).orElse((byte) 0));
        player.sendMessage("Occupation Exp: {} / {}", occupation.map(Occupation::getExperience).orElse(0), occupation.map(o -> o.getType().getExperienceForLv(o.getLevel())).orElse(0));
        player.sendMessage("========== Points ==========");
        player.sendMessage("Fishing Points: {}", StringUtil.formatNumber(target.getFishingPoints()));
        player.sendMessage("Event Points: {}", StringUtil.formatNumber(target.getEventPoints()));
        player.sendMessage("Donor Points: {}", StringUtil.formatNumber(target.getClient().getDonationPoints()));
        player.sendMessage("Vote points: {}", StringUtil.formatNumber(target.getClient().getVotePoints()));
    }

    private void SetStat(MapleCharacter player, Command cmd, CommandArgs args) {
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
            switch (cmd.getName()) {
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
            player.dropMessage(5, String.format("Syntax: @%s <amount>", cmd.getName()));
        }
    }

    private void ResetStats(MapleCharacter player, Command cmd, CommandArgs args) {
        String statName = cmd.getName().substring(5);

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
    }

    private void RPS(MapleCharacter player, Command cmd, CommandArgs args) {
        RockPaperScissorsEvent.startGame(player);
        player.dropMessage(6, "Let's play some rock paper scissors!");
    }

    private void Report(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 1) {
            String username = args.get(0);
            String message = args.concatFrom(1);
            MapleWorld world = player.getClient().getWorldServer();
            MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
            if (target != null) {
                world.sendMessageIf(p -> p.getGMLevel() > 0, 6, "[Report] %s : (%s) %s", player.getName(), username, message);
            } else {
                player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
            }
        } else {
            player.dropMessage(5, "You must specify a username and message");
        }
    }

    private void CallGM(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            String message = args.concatFrom(0);
            if (!message.isEmpty()) {
                String content = String.format("%s: %s", player.getName(), message);
                player.getClient().getWorldServer().sendMessageIf(p -> p.getGMLevel() > 0, 6, "[GM-Call] {}", content);
                player.dropMessage(6, "Help message sent");
            } else {
                player.dropMessage(5, "You must specify a message");
            }
        } else {
            player.dropMessage(5, "You must specify a message");
        }
    }

    private void Style(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 9900001);
    }

    private void Go(MapleCharacter player, Command cmd, CommandArgs args) {
        WeakHashMap<String, Integer> maps = new WeakHashMap<>();
        maps.put("shenron", 908);
        maps.put("arcade", 978);
        maps.put("quay", 541000000);
        maps.put("boatquay", 541000000);
        maps.put("henesys", 100000000);
        maps.put("maya", 100000001);
        maps.put("ellinia", 101000000);
        maps.put("perion", 102000000);
        maps.put("kerning", 103000000);
        maps.put("lith", 104000000);
        maps.put("harbor", 104000000);
        maps.put("lithharbor", 104000000);
        maps.put("florina", 110000000);
        maps.put("nautilus", 120000000);
        maps.put("ereve", 130000000);
        maps.put("rien", 140000000);
        maps.put("orbis", 200000000);
        maps.put("elnath", 211000000);
        maps.put("ludi", 220000000);
        maps.put("aqua", 230000000);
        maps.put("leafre", 240000000);
        maps.put("mulung", 250000000);
        maps.put("herb", 251000000);
        maps.put("herbtown", 251000000);
        maps.put("ariant", 260000000);
        maps.put("timetemple", 270000000);
        maps.put("magatia", 261000000);
        maps.put("ellin", 300000000);
        maps.put("nlc", 600000000);
        maps.put("amoria", 680000000);
        maps.put("home", 910000000);
        maps.put("fm", 910000000);

        Integer mapID = cmd.getName().equalsIgnoreCase("go") ? maps.get(args.get(0)) : maps.get(cmd.getName());
        if (mapID != null) {
            player.changeMap(player.getClient().getChannelServer().getMap(mapID));
        } else {
            StringBuilder sb = new StringBuilder();
            for (String s : maps.keySet()) {
                sb.append(s).append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.dropMessage(5, "These are the current maps available for you to warp to");
            player.dropMessage(5, sb.toString());
            maps.clear();
        }
    }

    private void Online(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() == 0) {
            for (MapleChannel channel : world.getChannels()) {
                int playerCount = 0;
                StringBuilder sb = new StringBuilder();
                Collection<MapleCharacter> players = channel.getPlayers();
                for (MapleCharacter online : players) {
                    if (!online.isGM() || !online.isHidden()) {
                        playerCount++;
                        sb.append(online.getName()).append(", ");
                    }
                }
                players.clear();
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 2);
                }
                player.sendMessage("Channel {} - {} players", channel.getId(), playerCount);
                player.sendMessage("{}", sb.toString());
            }
        } else if (args.get(0).equalsIgnoreCase("npc")) {
            StringBuilder sb = new StringBuilder();
            for (MapleChannel channel : world.getChannels()) {
                Collection<MapleCharacter> players = channel.getPlayers();
                sb.append("#echannel ").append(channel.getId()).append(" - ");
                int playerCount = 0;
                StringBuilder usernames = new StringBuilder();
                for (MapleCharacter online : players) {
                    if (!online.isGM() || !online.isHidden()) {
                        playerCount++;
                        usernames.append(online.getName()).append(" ");
                    }
                }
                players.clear();
                sb.append(playerCount).append(" players#n\r\n");
                sb.append(usernames.toString());
                sb.append("\r\n");
                usernames.setLength(0);
            }
            player.getClient().announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        }
    }

    private void Home(MapleCharacter player, Command cmd, CommandArgs args) {
        player.saveLocation(SavedLocationType.FREE_MARKET.name());
        player.changeMap(ServerConstants.HOME_MAP);
    }

    private void Achievements(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 2007, "f_achievements");
    }

    private void Dispose(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.dispose(player.getClient());
        player.announce(MaplePacketCreator.enableActions());
        player.getMap().getMonsters().stream().filter(m -> !m.isAlive()).forEach(m -> player.getMap().killMonster(m, null, false));
        player.dropMessage(6, "Disposed");
    }

    private void ParticipateAutoEvent(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean join = cmd.equals("jautoevent");
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
    }

    private void ParticipateEvent(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleClient client = player.getClient();

        boolean join = cmd.equals("joinevent");
        ManualPlayerEvent playerEvent = client.getWorldServer().getPlayerEvent();
        if (playerEvent != null) {
            if (!playerEvent.isOpen()) {
                player.sendMessage(5, "The event is no longer open");
                return;
            } else if (client.getChannel() != playerEvent.getChannel().getId()) {
                player.sendMessage(5, "The event is being hosted ein channel {}", playerEvent.getChannel().getId());
                return;
            }
            if (join) {
                if (player.getMap() != playerEvent.getMap() && !playerEvent.participants.containsKey(player.getId())) {
                    ManualPlayerEvent.Participant p = new ManualPlayerEvent.Participant(player.getId(), player.getMapId());
                    playerEvent.participants.put(player.getId(), p);
                    player.changeMap(playerEvent.getMap(), playerEvent.getSpawnPoint());
                } else {
                    player.sendMessage(5, "You are already in the event");
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
    }

    private void Help(MapleCharacter player, Command cnd, CommandArgs args) {
        boolean npc = args.length() == 1 && args.get(0).equals("npc");
        ArrayList<String> commands = new ArrayList<>(getCommandCount());
        commands.add("@help - View all commands");
        commands.add("@joinevent - Join the GM hosted event");
        commands.add("@leave - Leave the GM hosted event");
        commands.add("@jautoevent - Join the automated event");
        commands.add("@lautoevent - Leave the automated event");
        commands.add("@dispose - For troubles with game interactions");
        commands.add("@achievements - View known achievements");
        commands.add("@home - Teleport to the server home map");
        commands.add("@online - View all online players");
        commands.add("@go - Teleport to several town maps");
        commands.add("@shenron - Teleport to the Shenron summoning map");
        commands.add("@arcade - Teleport to the arcade map");
        commands.add("@style - View the player stylist NPC");
        commands.add("@callgm - Request help from any online GM");
        commands.add("@report - Report a player");
        commands.add("@rps - Begin a game of Rock, Paper, Scissors");
        commands.add("@reset<str/dex/int/luk> - Reset a stat and return AP");
        commands.add("@<str/dex/int/luk> - Distribute AP into stats");
        commands.add("@<check/checkme> - View some of your player data");
        commands.add("@spy - View another player's data");
        commands.add("@maxskills - Set all skills to maximum level");
        commands.add("@fixexp - Reset your experience to 0");
        commands.add("@quests - View known and started custom quests");
        commands.add("@afk - Set your player state to AFK");
        commands.add("@uptime - View how long the server is been online");
        commands.add("@time - View the server's time in it's timezone");
        commands.add("@house - View the housing system NPC");
        commands.add("@jobs - View a list of the customized jobs");
        commands.add("@chalktalk - Created a chalkboard with a message above your player");
        commands.add("@rebirth - Do a rebirth for your player");
        commands.add("@pvp - Enable or disable PVP for your player");
        commands.add("@bosshp - View HP for all boss monsters in the map");
        commands.add("@ranks - View rankings for a specific data type");
        commands.add("@trade<jq/ep/vp> - Give your points to another player");
        commands.sort(String::compareTo);
        if (npc) {
            StringBuilder sb = new StringBuilder();
            commands.forEach(s -> sb.append("\r\n").append(s));
            player.announce(MaplePacketCreator.getNPCTalk(2007, (byte) 0, sb.toString(), "00 00", (byte) 0));
        } else {
            commands.forEach(player::dropMessage);
            player.dropMessage("If you'd like to view this list in an NPC window, use the command < @help npc >");
        }
        commands.clear();
    }
}
