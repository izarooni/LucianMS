package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.MapleStat;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.RockPaperScissorsEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.features.PlayerBattle;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.FakePlayer;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleSummon;
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
import java.util.concurrent.TimeUnit;

/**
 * @author izarooni, lucasdieswagger
 */
public class PlayerCommands extends CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerCommands.class);
    private static ArrayList<String> HELP_LIST;
    private TreeMap<String, Integer> MAPS;

    public static final String TAG_TOGGLE = "cmd_tag";
    public static final String BOMB_TOGGLE = "cmd_bomb";

    private static void CollectLeaderboard(Connection con, List<Pair<String, Integer>> usernames, String query) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usernames.add(new Pair<>(rs.getString("name"), rs.getInt("value")));
                }
            }
        }
    }

    public PlayerCommands() {
        addCommand("help", this::Help, "Show a list of player commands");
        addCommand("commands", this::Help, "Show a list of player commands");
        addCommand("chirithy", this::Chirithy, "Opens the Chirithy NPC");

        addCommand("joinevent", this::ParticipateEvent, "Join a GM hosted event");
        addCommand("leaveevent", this::ParticipateEvent, "Leave a GM hosted event");

        addCommand("jautoevent", this::ParticipateAutoEvent, "Join an auto-event");
        addCommand("lautoevent", this::ParticipateAutoEvent, "Leave an auto-event");

        addCommand("dispose", this::Dispose, "If you are unable to talk to NPCs or use portals");
        addCommand("achievements", this::Achievements, "View available achievements and check unclaimed rewards");
        addCommand("home", this::Home, "Fast warp to the server home map");
        addCommand("online", this::Online, "View a list of online players");

        addCommand("go", this::Go, "Fast warp to towns and features");
        addCommand("shenron", this::Go, "Fast warp to Shenron's summoning map");
        addCommand("arcade", this::Go, "Fast warp to the arcade system map");

        addCommand("style", this::Style, "Opens the stylist NPC");
        addCommand("callgm", this::CallGM, "Send a help message to all GMs online");
        addCommand("report", this::Report, "Send a player report to all GMs online");
//        addCommand("rps", this::RPS, "Play a quick game of Rock, Paper, Scissors");

        addCommand("resetstats", this::ResetStats, "Resets all stats to 4 points and returns AP");
        addCommand("resetstr", this::ResetStats, "Resets STR to 4 points and returns AP");
        addCommand("resetdex", this::ResetStats, "Resets DEX to 4 points and returns AP");
        addCommand("resetint", this::ResetStats, "Resets INT to 4 points and returns AP");
        addCommand("resetluk", this::ResetStats, "Resets LUK to 4 points and returns AP");

        addCommand("str", this::SetStat, "Distribute Ability Points into STR");
        addCommand("dex", this::SetStat, "Distribute Ability Points into DEX");
        addCommand("int", this::SetStat, "Distribute Ability Points into INT");
        addCommand("luk", this::SetStat, "Distribute Ability Points into LUK");

        addCommand("checkme", this::CheckPlayer, "Check stats of your character");
        addCommand("spy", this::CheckPlayer, "Check stats of another character");

        addCommand("fixexp", this::FixExp, "Resets your EXP to 0");
        addCommand("quests", this::Quests, "View available and completed custom quests");
//        addCommand("afk", this::SetAFK);
        addCommand("uptime", this::ServerUptime, "View how long the server has been online");
        addCommand("time", this::ServerTime, "View the server's current time");
        addCommand("house", this::House, "Opens the housing management NPC");
        addCommand("jobs", this::Jobs, "View a list of current custom and cover-up jobs");
        addCommand("chalktalk", this::ChalkTalk, "Display a chalkboard with a message above your character");
        addCommand("rebirth", this::Rebirth, "Rebirth your character");
        addCommand("pvp", this::InitPlayerBattle, "Enable to disable PvP mode (Try out our PvP maps for a neat feature!)");
        addCommand("bosshp", this::BossHP, "View the HP of all mob bosses in the map");
        addCommand("ranks", this::Ranks, "View the top 25 rankings for certain categories");

        addCommand("tradeep", this::TradePoints, "Trade your Event Points with another player");
        addCommand("tradevp", this::TradePoints, "Trade your Vote Points with another player");
        addCommand("tradejq", this::TradePoints, "Trade your Jump Quest Points with another player");
        addCommand("autorb", this::ToggleAutoRebirth, "Toggle the auto-rebirth ability");

        addCommand("ping", this::Ping, "View your Round-Trip delay with the server");
        addCommand("emo", this::Emo, "Kills your character");
        addCommand("debuff", this::Debuff, "Removes all buffs and summons");

        addCommand("tag", this::Tag, "Tag nearby players");
        addCommand("bomb", this::Bomb, "Spawns a bomb on your character");

        MAPS = new TreeMap<>();
        MAPS.put("amoria", 680000000);
        MAPS.put("aqua", 230000000);
        MAPS.put("arcade", 978);
        MAPS.put("ariant", 260000000);
        MAPS.put("boatquay", 541000000);
        MAPS.put("ellin", 300000000);
        MAPS.put("ellinia", 101000000);
        MAPS.put("elnath", 211000000);
        MAPS.put("ereve", 130000000);
        MAPS.put("florina", 110000000);
        MAPS.put("fm", 910000000);
        MAPS.put("guild", 200000301);
        MAPS.put("harbor", 104000000);
        MAPS.put("henesys", 100000000);
        MAPS.put("herb", 251000000);
        MAPS.put("herbtown", 251000000);
        MAPS.put("home", 910000000);
        MAPS.put("kerning", 103000000);
        MAPS.put("leafre", 240000000);
        MAPS.put("lith", 104000000);
        MAPS.put("ludi", 220000000);
        MAPS.put("magatia", 261000000);
        MAPS.put("maya", 100000001);
        MAPS.put("mulung", 250000000);
        MAPS.put("nautilus", 120000000);
        MAPS.put("nlc", 600000000);
        MAPS.put("omega", 221000000);
        MAPS.put("orbis", 200000000);
        MAPS.put("perion", 102000000);
        MAPS.put("pq", 910002000);
        MAPS.put("quay", 541000000);
        MAPS.put("rien", 140000000);
        MAPS.put("shenron", 908);
        MAPS.put("timetemple", 270000000);

        Map<String, Pair<CommandEvent, String>> commands = getCommands();
        HELP_LIST = new ArrayList<>(commands.size());
        for (Map.Entry<String, Pair<CommandEvent, String>> e : commands.entrySet()) {
            HELP_LIST.add(String.format("@%s - %s", e.getKey(), e.getValue().getRight()));
        }
        HELP_LIST.sort(String::compareTo);
    }

    private void Bomb(MapleCharacter player, Command cmd, CommandArgs args) {
        if (!((boolean) player.getMap().getVariables().checkProperty(BOMB_TOGGLE, false))) {
            if (!player.getToggles().checkProperty(BOMB_TOGGLE, false)) {
                player.sendMessage("This command is not enabled for you");
                return;
            }
        }
        MapleMonster bomb = MapleLifeFactory.getMonster(ServerConstants.GAME.BombMonster);
        if (bomb == null) {
            player.dropMessage(5, "An error occurred");
            return;
        }
        bomb.getStats().getSelfDestruction().setRemoveAfter(1000);
        player.getMap().spawnMonsterOnGroudBelow(bomb, player.getPosition());
    }

    private void Tag(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleMap map = player.getMap();
        if (!((boolean) map.getVariables().checkProperty(TAG_TOGGLE, false))) {
            if (!player.getToggles().checkProperty(TAG_TOGGLE, false)) {
                player.sendMessage("This command is not enabled for you");
                return;
            }
        }
        List<MapleCharacter> players = map.getPlayers(p -> (p.getGMLevel() == 0 || p.isDebug()) && p.getId() != player.getId());
        for (MapleCharacter target : players) {
            if (target.getPosition().distance(player.getPosition()) <= map.getTagRange()) {
                target.setHpMp(0);
                map.sendMessage(6, "{} is been tagged", target.getName());
            }
        }
        players.clear();
    }

    private void Chirithy(MapleCharacter player, Command cmd, CommandArgs args) {
        if (!JailManager.isJailed(player.getId())) {
            NPCScriptManager.start(player.getClient(), 2007, "f_multipurpose");
        } else {
            player.sendMessage(5, "You cannot do that while jailed.");
        }
    }

    private void Debuff(MapleCharacter player, Command cmd, CommandArgs args) {
        player.cancelAllBuffs();
        if (!player.getSummons().isEmpty()) {
            for (MapleSummon summon : player.getSummons().values()) {
                player.getMap().sendPacket(MaplePacketCreator.removeSummon(summon, true));
                player.getMap().removeMapObject(summon);
                summon.dispose();
            }
            player.getSummons().clear();
        }
    }

    private void Emo(MapleCharacter player, Command cmd, CommandArgs args) {
        player.setHpMp(0);
    }

    private void ToggleAutoRebirth(MapleCharacter player, Command cmd, CommandArgs args) {
        if (player.isDebug() || player.haveItem(ServerConstants.getAutoRebirthItem())) {
            player.setAutoRebirth(!player.isAutoRebirth());
            player.sendMessage("Auto-rebirth is now {}", (player.isAutoRebirth() ? "enabled" : "disabled"));
        } else {
            String name = MapleItemInformationProvider.getInstance().getName(ServerConstants.getAutoRebirthItem());
            player.sendMessage("You need the auto-rebirth item '{}' to use this command", name);
        }
    }

    private void Ping(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleClient client = player.getClient();
        client.setNetworkLatency(-1);
        client.setKeepAliveRequest(System.currentTimeMillis());
        client.announce(MaplePacketCreator.getKeepAliveRequest());
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
        ArrayList<Pair<String, Integer>> usernames = new ArrayList<>(10);
        String rankingType = args.get(0);
        try (Connection con = player.getClient().getWorldServer().getConnection()) {
            switch (rankingType) {
                default:
                    player.sendMessage("'{}' is not a valid leaderboard", rankingType);
                    return;
                case "rebirths":
                case "rebirth":
                case "rb":
                    CollectLeaderboard(con, usernames, "select name, reborns as value from characters where gm = 0 order by reborns desc limit 25");
                    break;
                case "coins":
                case "chirithy":
                    CollectLeaderboard(con, usernames, "select c.name, sum(i.quantity) as value from inventoryitems i inner join characters c on c.id = i.characterid and itemid = " + ServerConstants.GAME.SoftCurrency + " and gm = 0 group by characterid order by value desc limit 25");
                    break;
                case "event":
                case "ep":
                    CollectLeaderboard(con, usernames, "select name, eventpoints as value from characters where gm = 0 order by value desc limit 25");
                    break;
                case "jq":
                    CollectLeaderboard(con, usernames, "select name, jumpquestpoints as value from characters where gm = 0 order by value desc limit 25");
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
            for (Pair<String, Integer> p : usernames) {
                w.writeMapleString(p.getLeft());
                w.writeInt(p.getRight());
                w.writeInt(0);
                w.writeInt(0);
                w.writeInt(0);
                w.writeInt(0);
            }
            usernames.clear();
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
        player.sendMessage("Currency: {}", target.getItemQuantity(ServerConstants.GAME.SoftCurrency, false));
        Optional<Occupation> occupation = Optional.ofNullable(target.getOccupation());
        player.sendMessage("Rebirths: {}", target.getRebirths());
        player.sendMessage("Occupation: {} Lv.{}", occupation.map(o -> o.getType().name()).orElse("N/A"), occupation.map(Occupation::getLevel).orElse((byte) 0));
        player.sendMessage("Occupation Exp: {} / {}", occupation.map(Occupation::getExperience).orElse(0), occupation.map(o -> o.getType().getExperienceForLv(o.getLevel())).orElse(0));
        if (target.isImmortal()) {
            long endAt = (target.getImmortalTimestamp() + TimeUnit.HOURS.toMillis(1));
            player.sendMessage("Immortal for {}", StringUtil.getTimeElapse(endAt - System.currentTimeMillis()));
        }
        FakePlayer fakePlayer = target.getFakePlayer();
        if (fakePlayer != null && fakePlayer.getExpiration() > 0) {
            player.sendMessage("Clone expires in {}", StringUtil.getTimeElapse(fakePlayer.getExpiration() - System.currentTimeMillis()));
        }
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
        Integer mapID = cmd.getName().equalsIgnoreCase("go") ? MAPS.get(args.get(0)) : MAPS.get(cmd.getName());
        if (mapID != null) {
            player.changeMap(player.getClient().getChannelServer().getMap(mapID));
        } else {
            StringBuilder sb = new StringBuilder();
            for (String s : MAPS.keySet()) {
                sb.append(s).append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.dropMessage(5, "These are the current maps available for you to warp to");
            player.dropMessage(5, sb.toString());
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
        player.changeMap(ServerConstants.MAPS.Home);
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

    private void Help(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean npc = args.length() == 1 && args.get(0).equalsIgnoreCase("npc");
        if (npc) {
            StringBuilder sb = new StringBuilder();
            for (String s : HELP_LIST) {
                String[] split = s.split(" - ");
                sb.append("\r\n#b").append(split[0]).append("#k - #r").append(split[1]);
            }
            player.announce(MaplePacketCreator.getNPCTalk(2007, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        } else {
            HELP_LIST.forEach(player::dropMessage);
        }
    }
}
