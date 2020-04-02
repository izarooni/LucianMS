package com.lucianms.command.executors;

import com.lucianms.BanManager;
import com.lucianms.LChannelMain;
import com.lucianms.client.*;
import com.lucianms.client.inventory.*;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.ServerConstants;
import com.lucianms.helpers.JailManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.InterPacketOperation;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
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
import tools.StringUtil;

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
public class GameMasterCommands extends CommandExecutor {

    private static ArrayList<String> HELP_LIST;

    public GameMasterCommands() {
        addCommand("gmcmds", this::CommandList, "");
        addCommand("fwarp", this::ForceWarp, "");
        addCommand("hair", this::ChangeHair, "");
        addCommand("eye", this::ChangeFace, "");
        addCommand("killed", this::Killed, "");
        addCommand("itemq", this::ItemQ, "");
        addCommand("stalker", this::Stalker, "");
        addCommand("gmmap", this::GMHideout, "");
        addCommand("dc", this::Disconnect, "");
        addCommand("wm", this::WarpMap, "");
        addCommand("wmx", this::WarpMap, "");
        addCommand("warp", this::Warp, "");
        addCommand("wh", this::WarpHere, "");
        addCommand("whx", this::WarpHere, "");
        addCommand("clock", this::Clock, "");
        addCommand("mutem", this::MuteMap, "");
        addCommand("unmutem", this::MuteMap, "");
        addCommand("say", this::WorldSpeak, "");
        addCommand("mute", this::Mute, "");
        addCommand("unmute", this::Mute, "");
        addCommand("job", this::Job, "");
        addCommand("hp", this::SetHealth, "");
        addCommand("mp", this::SetMana, "");
        addCommand("levelup", this::Levelup, "");
        addCommand("spawn", this::SpawnMonster, "");
        addCommand("level", this::SetLevel, "");
        addCommand("ban", this::Ban, "");
        addCommand("banaccount", this::AccountBan, "");
        addCommand("unban", this::Unban, "");
        addCommand("tagrange", this::SetTagRange, "");
        addCommand("tag", this::Tag, "");
        addCommand("maxskills", this::MaxSkills, "");
        addCommand("heal", this::Heal, "");
        addCommand("healm", this::Heal, "");
        addCommand("notice", this::Notice, "");
        addCommand("gift", this::Gift, "");
        addCommand("kill", this::Kill, "");
        addCommand("killmap", this::KillMap, "");
        addCommand("reloadmap", this::ReloadMap, "");
        addCommand("killall", this::KillAll, "");
        addCommand("mobhp", this::MobHP, "Lists all mob HP and IDs");
        addCommand("cleardrops", this::ClearDrops, "");
        addCommand("clearinv", this::ClearInventory, "");
        addCommand("hide", this::SetHide, "");
        addCommand("maxstats", this::MaxStats, "");
        addCommand("dispel", this::Dispel, "");
        addCommand("online", this::Online, "");
        addCommand("!", this::GMChat, "");
        addCommand("itemvac", this::ItemVac, "");
        addCommand("characters", this::Characters, "");
        addCommand("jail", this::Jail, "");
        addCommand("unjail", this::Jail, "");
        addCommand("search", this::Search, "");
        addCommand("chattype", this::ChatType, "");
        addCommand("ap", this::SetAP, "");
        addCommand("sp", this::SetSP, "");
        addCommand("buff", this::Buff, "");
        addCommand("fame", this::SetFame, "");
        addCommand("mesos", this::SetMesos, "");
        addCommand("gender", this::SetGender, "");
        addCommand("setall", this::SetStats, "");
        addCommand("occupation", this::SetOccupation, "");
        addCommand("item", this::CreateItem, "");
        addCommand("drop", this::CreateDrop, "");
        addCommand("multiuser", this::MultiUser, "Sees if anyone is multi-clienting");
        addCommand("smega", this::Smega, "Test command tbh");

        Map<String, Pair<CommandEvent, String>> commands = getCommands();
        HELP_LIST = new ArrayList<>(commands.size());
        for (Map.Entry<String, Pair<CommandEvent, String>> e : commands.entrySet()) {
            HELP_LIST.add(String.format("!%s - %s", e.getKey(), e.getValue().getRight()));
        }
        HELP_LIST.sort(String::compareTo);
    }

    private void Smega(MapleCharacter player, Command cmd, CommandArgs args) {
        String medal = "";
        Item medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
        if (medalItem != null) {
            medal = "<" + MapleItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
        }
        player.getClient().getWorldServer().sendPacket(MaplePacketCreator.serverNotice(3, player.getClient().getChannelServer().getId(), String.format("%s%s : %s", medal, player.getName(), args.concatFrom(0)), true));

    }

    private void WorldSpeak(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            MapleWorld world = player.getClient().getWorldServer();
            if (cmd.equals("say")) {
                world.sendMessage(6, "{} : {}", player.getName(), args.concatFrom(0));
            } else {
                world.sendPacket(MaplePacketCreator.earnTitleMessage(args.concatFrom(0)));
            }
        } else {
            player.dropMessage("You must enter a message");
        }
    }

    private void CommandList(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean npc = args.length() == 1 && args.get(0).equalsIgnoreCase("npc");
        if (npc) {
            StringBuilder sb = new StringBuilder();
            for (String s : HELP_LIST) {
                String[] split = s.split(" - ");
                sb.append("\r\n#b").append(split[0]).append("#k - #r");
                if (split.length == 2) sb.append(split[1]);
            }
            player.announce(MaplePacketCreator.getNPCTalk(2007, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        } else {
            HELP_LIST.forEach(player::dropMessage);
        }
    }

    private void MobHP(MapleCharacter player, Command cmd, CommandArgs args) {

        for (MapleMonster monster : player.getMap().getMonsters()) {

            player.sendMessage(5, "Name: '{}', Level: {}, HP: {} / {}, ID: {}",
                    monster.getName(), monster.getLevel(),
                    StringUtil.formatNumber(monster.getHp()), StringUtil.formatNumber(monster.getMaxHp()), StringUtil.formatNumber(monster.getId()));

        }

    }

    private void ForceWarp(MapleCharacter player, Command cmd, CommandArgs args) {
        Integer fieldId = args.parseNumber(0, int.class);
        if (fieldId != null) {
            player.announce(MaplePacketCreator.getWarpToMap(fieldId, 0x80, player, null));
        } else {
            player.sendMessage(5, "Syntax: !fwarp <map_id>");
        }
    }

    private void Killed(MapleCharacter player, Command cmd, CommandArgs args) {
        if (player.getMap().getLastPlayerDiedInMap() != null) {
            player.dropMessage(6, "The last player who died on this map is " + player.getMap().getLastPlayerDiedInMap());
        } else {
            player.dropMessage(5, "Nobody died here.. yet. Perhaps you should try dying yourself?");
        }
    }

    private void SpawnMonster(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            Integer monsterId = args.parseNumber(0, int.class);
            Integer amount = args.parseNumber(1, 1, int.class);
            Long hp = args.parseNumber(args.findArg("hp"), long.class);
            Integer exp = args.parseNumber(args.findArg("exp"), int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            for (int i = 0; i < Math.min(500, amount); i++) {
                MapleMonster monster = MapleLifeFactory.getMonster(monsterId);
                if (monster == null) {
                    player.dropMessage(5, String.format("'%d' is not a valid monster", monsterId));
                    return;
                }
                if (args.length() > 3) {
                    MapleMonsterStats stats = new MapleMonsterStats();
                    stats.setHp(hp == null ? monster.getHp() : hp);
                    if (exp != null && exp < 0) {
                        exp = monster.getExp() * Math.abs(exp);
                    }
                    stats.setExp(exp == null ? monster.getExp() : exp);
                    monster.setOverrideStats(stats);
                }
                monster.setFh(player.getFoothold());
                player.getMap().spawnMonsterOnGroudBelow(monster, player.getPosition());
            }
        } else {
            player.dropMessage(5, "You must specify a monster ID");
        }
    }


    private void CreateItem(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            Integer id = args.parseNumber(0, int.class);
            Short quantity = args.parseNumber(1, (short) 1, short.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            long expiration = -1;
            int petId = -1;
            if (ItemConstants.isPet(id)) {
                petId = MaplePet.createPet(id);
                expiration = Long.MAX_VALUE;
            }
            if (MapleInventoryManipulator.addById(player.getClient(), id, quantity, player.getName(), petId, expiration)) {
                player.announce(MaplePacketCreator.getShowItemGain(id, quantity));
            } else {
                player.dropMessage(5, "Failed to create the item");
            }
        } else {
            player.dropMessage(5, "You need to specify the ID of the item you want");
        }
    }

    private void CreateDrop(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            Integer id = args.parseNumber(0, int.class);
            Short quantity = args.parseNumber(1, (short) 1, short.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            long expiration = -1;
            int petId = -1;
            if (ItemConstants.isPet(id)) {
                petId = MaplePet.createPet(id);
                expiration = Long.MAX_VALUE;
            }
            Item toDrop;
            if (ItemConstants.getInventoryType(id) == MapleInventoryType.EQUIP) {
                toDrop = MapleItemInformationProvider.getInstance().getEquipById(id);
            } else {
                toDrop = new Item(id, (byte) 0, quantity);
                if (petId > 0) {
                    toDrop.setExpiration(expiration);
                }
            }
            if (toDrop != null) {
                player.getMap().spawnItemDrop(player, player, toDrop, player.getPosition(), true, true);
            } else {
                player.dropMessage(5, "That item does not exist");
            }
        } else {
            player.dropMessage(5, "You need to specify the ID of the item you want.");
        }
    }

    private void ItemQ(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() >= 2) {
            StringBuilder sb = new StringBuilder();
            int itemId = args.parseNumber(0, int.class);
            int qnty = args.parseNumber(1, int.class);
            sb.append("Online users: \r\n\r\n");
            if (args.getFirstError() == null) {
                for (MapleCharacter target : world.getPlayerStorage().values()) {
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
                    try (Connection con = world.getConnection();
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
                player.dropMessage(5, "Syntax: !itemq <itemid> <morethan>");
                return;
            }
            if (!(sb.length() == 0)) {
                player.announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, sb.toString(), "00 00", (byte) 0));
                sb.setLength(0);
            } else {
                player.dropMessage(5, "Nobody found online with more than this amount.");
            }
        } else {
            player.dropMessage(5, "Syntax: !itemq <itemid> <morethan>");
        }
    }

    private void Stalker(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 10200, "t_stalker");
    }

    private void GMHideout(MapleCharacter player, Command cmd, CommandArgs args) {
        player.changeMap(331001000, 0);
    }

    private void Disconnect(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() == 1) {
            String username = args.get(0);
            MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
            if (target != null) {
                target.getClient().disconnect();
            } else {
                player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
            }
        } else {
            player.dropMessage(5, "You must specify a username");
        }
    }

    private void WarpMap(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean exact = cmd.getName().endsWith("x");
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
            map = player.getClient().getChannelServer().getMap(mapId);
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
    }

    private void Warp(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() == 0) {
            player.sendMessage(5, "Syntax: !{} <username> [username/map ID]", cmd.getName());
            return;
        }
        String username = args.get(0);
        MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
        if (target != null && args.length() == 1) {
            if (target.getClient().getChannel() != player.getClient().getChannel()) {
                player.setMap(target.getMapId());
                player.getClient().changeChannel(target.getClient().getChannel());
            } else {
                player.changeMap(target.getMap(), target.getPosition());
            }
        } else if (target != null && args.length() == 2) {
            Number number = args.parseNumber(1, int.class);
            if (number == null) {
                String destUsername = args.get(1);
                MapleCharacter dest = world.findPlayer(p -> p.getName().equalsIgnoreCase(destUsername));
                if (dest != null) {
                    if (target.getClient().getChannel() != player.getClient().getChannel()) {
                        dest.setMap(target.getMapId());
                        dest.getClient().changeChannel(target.getClient().getChannel());
                    } else {
                        dest.changeMap(target.getMap(), target.getPosition());
                    }
                } else {
                    player.sendMessage(5, "Could not warp {} to {}. {} not found", target.getName(), destUsername, destUsername);
                }
            } else {
                target.changeMap(number.intValue());
                player.sendMessage(6, "Warped {} to {}", target.getName(), number.intValue());
            }
        } else if (target == null) {
            Number number = args.parseNumber(0, int.class);
            if (number == null) {
                switch (args.get(0)) {
                    case "ox":
                        number = 109020001;
                        break;
                    default:
                        player.sendMessage(5, "Could not find any player named '{}' or it is not a number", username);
                        return;
                }
            }
            MapleMap map = player.getClient().getChannelServer().getMap(number.intValue());
            if (map != null) {
                player.changeMap(map);
            } else {
                player.sendMessage(5, "Could not find the map '{}' perhaps the server doesn't have it?", number.intValue());
            }
        }
    }

    private void WarpHere(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean exact = cmd.getName().endsWith("x");
        MapleWorld world = player.getClient().getWorldServer();
        String username = args.get(0);
        MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
        if (target != null) {
            if (target.getClient().getChannel() != player.getClient().getChannel()) {
                target.setMap(target.getMapId());
                target.getClient().changeChannel(player.getClient().getChannel());
            } else {
                if (exact) {
                    target.changeMap(player.getMap(), player.getPosition());
                } else {
                    target.changeMap(player.getMap());
                }
            }
        } else {
            player.sendMessage(5, "Could not find any player named {}", username);
        }
    }

    private void Clock(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() != 1) {
            player.sendMessage(5, "Syntax: !{} <time (in seconds)", cmd.getName());
            return;
        }
        Integer time = args.parseNumber(0, int.class);
        String error = args.getFirstError();
        if (error != null) {
            player.dropMessage(5, error);
            return;
        }
        player.getMap().broadcastMessage(MaplePacketCreator.getClock(time));
    }

    private void MuteMap(MapleCharacter player, Command cmd, CommandArgs args) {
        boolean mute = cmd.equals("mutemap", "mutem");
        String muteText = (mute ? "muted" : "unmuted");
        for (MapleCharacter players : player.getMap().getCharacters()) {
            if (!players.isGM() || player.isDebug()) {
                players.setMuted(mute);
                players.sendMessage(5, "You have been {}", muteText);
            }
        }
        player.sendMessage(5, "The map has been {}", (mute ? "muted" : "unmuted"));
    }

    private void Mute(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() > 0) {
            boolean mute = cmd.equals("mute");
            String muteText = (mute ? "muted" : "unmuted");
            for (int i = 0; i < args.length(); i++) {
                final String username = args.get(i);
                MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    target.setMuted(mute);
                    player.sendMessage(6, "'{}' has been {}", target.getName(), muteText);
                    target.sendMessage(6, "You have been {}", muteText);
                } else {
                    player.sendMessage(5, "Could not find any player named {}", username);
                }
            }
        } else {
            player.sendMessage(5, "Syntax: !{} <usernames...>", cmd.getName());
        }
    }

    private void ChangeHair(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            Integer hair = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }

            player.setHair(hair);
            player.updateSingleStat(MapleStat.HAIR, hair);
            player.equipChanged(true);
        }
    }

    private void ChangeFace(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            Integer face = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }

            player.setFace(face);
            player.updateSingleStat(MapleStat.FACE, face);
            player.equipChanged(true);
        }
    }

    private void Job(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            Integer jobId = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            MapleJob job = MapleJob.getById(jobId);
            if (job == null) {
                player.sendMessage("{} is not a valid job", args.get(0));
                return;
            }
            if (args.length() > 1) {
                for (int i = 1; i < args.length(); i++) {
                    String username = args.get(i);
                    MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        target.setJob(job);
                        target.updateSingleStat(MapleStat.JOB, job.getId());
                    } else {
                        player.sendMessage(5, "Unable to find any player named {}", username);
                    }
                }
            } else {
                player.setJob(job);
                player.updateSingleStat(MapleStat.JOB, job.getId());
            }
        } else {
            player.sendMessage(5, "Syntax: !{} <job_id> [username]", cmd.getName());
        }
    }

    private void SetHealth(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            player.sendMessage(5, "Syntax: !{} <value>", cmd.getName());
            return;
        }
        Number n = args.parseNumber(0, int.class);
        if (n == null) {
            player.sendMessage(5, "{} is not a number", args.get(0));
            return;
        }
        int value = n.intValue();
        player.setMaxHp(value);
        player.setHp(value);
        player.updateSingleStat(MapleStat.MAXHP, value);
        player.updateSingleStat(MapleStat.HP, value);
    }

    private void SetMana(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            player.sendMessage(5, "Syntax: !{} <value>", cmd.getName());
            return;
        }
        Number n = args.parseNumber(0, int.class);
        if (n == null) {
            player.sendMessage(5, "{} is not a number", args.get(0));
            return;
        }
        int value = n.intValue();
        player.setMaxMp(value);
        player.setMp(value);
        player.updateSingleStat(MapleStat.MAXMP, value);
        player.updateSingleStat(MapleStat.MP, value);
    }

    private void Levelup(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        Integer levels = args.parseNumber(0, int.class);
        if (args.length() > 0 && levels == null) {
            player.sendMessage(args.getFirstError());
            return;
        }
        if (args.length() == 2) {
            for (int i = 1; i < args.length(); i++) {
                String username = args.get(i);
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    target.gainLevels(levels);
                    player.sendMessage(5, "'{}' has leveled up {} times", target.getName(), levels);
                } else {
                    player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                }
            }
        } else if (args.length() < 2) {
            player.gainLevels(levels == null ? 1 : levels);
        } else {
            player.sendMessage(5, "Syntax: !{} <username> <levels>", cmd.getName());
        }
    }

    private void SetLevel(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

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
                    MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        target.setLevel(level);
                        target.setExp(0);
                        target.updateSingleStat(MapleStat.LEVEL, target.getLevel());
                        target.updateSingleStat(MapleStat.EXP, target.getExp());
                        target.dropMessage("Your level has been updated to " + target.getLevel());
                    } else {
                        player.dropMessage(String.format("Unable to find any player named '%s'", username));
                    }
                }
            } else {
                player.setLevel(level);
                player.setExp(0);
                player.updateSingleStat(MapleStat.LEVEL, player.getLevel());
                player.updateSingleStat(MapleStat.EXP, player.getExp());
            }
        } else {
            player.dropMessage(5, "Syntax: !level <number> [usernames...]");
        }
    }

    private void Ban(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() > 1) {
            String username = args.get(0);
            String reason = args.concatFrom(1);
            MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));

            if (target != null) {
                BanManager.setBanned(target.getAccountID(), reason);
                target.getClient().disconnect();
                player.sendMessage(6, "'{}' has been banned", username);
            } else {
                int accountID = MapleCharacter.getAccountIdByName(username);
                if (accountID > 0) {
                    BanManager.setBanned(accountID, reason);
                    player.sendMessage(6, "Offline banned '{}'", username);
                } else {
                    player.sendMessage("Unable to find any player named '{}'", username);
                }
            }

        } else {
            player.dropMessage(5, "You must specify a username and a reason");
        }
    }

    private void AccountBan(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        String username = args.get(0);
        String reason = args.concatFrom(1);
        int accountid = 0;
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accountid = rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            //LOGGER.error("Unable to get account ID of player '{}'", name);
        }
        BanManager.setBanned(accountid, reason);

        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE accountid = ?")) {
            ps.setInt(1, accountid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String charname = rs.getString("name");
                    MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(charname));
                    if (target != null) {
                        target.getClient().disconnect();
                    }
                }
            }
        } catch (SQLException e) {
            //LOGGER.error("Unable to get account ID of player '{}'", name);
        }

        player.sendMessage(6, "'{}' has been banned", username);
    }

    private void Unban(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() != 1) {
            player.dropMessage("Syntax: !unban <username>");
            return;
        }
        String username = args.get(0);
        if (BanManager.pardonUser(username)) {
            MaplePacketWriter w = new MaplePacketWriter();
            w.write(InterPacketOperation.BanManager.ordinal());
            w.writeMapleString(username);
            LChannelMain.getCommunicationsHandler().sendPacket(w.getPacket());
            player.sendMessage("'{}' has successfully been unbanned", username);
        } else {
            player.sendMessage("Failed to find any account via username '{}'", username);
        }
    }

    private void SetTagRange(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            Integer range = args.parseNumber(0, int.class);
            if (range == null) {
                player.dropMessage(5, args.getFirstError());
                return;
            }
            player.getMap().setTagRange(range);
            player.sendMessage("Tag range set to {}", range);
        } else {
            player.sendMessage("The tag range is currently set to {}", player.getMap().getTagRange());
        }
    }

    private void Tag(MapleCharacter player, Command cmd, CommandArgs args) {
        for (MapleMapObject obj : player.getMap().getMapObjectsInRange(player.getPosition(), player.getMap().getTagRange(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
            MapleCharacter chrs = (MapleCharacter) obj;
            if (chrs != player && (!chrs.isGM() || chrs.isDebug())) {
                chrs.setHpMp(0);
                chrs.dropMessage(6, "You have been tagged!");
            }
        }
    }

    private void MaxSkills(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleCharacter target = player;
        if (args.length() == 1) {
            target = player.getClient().getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target == null) {
                player.sendMessage("Unablee to find any player named {}", args.get(0));
                return;
            }
        }
        for (Skill skill : SkillFactory.getSkills().values()) {
            target.getSkills().put(skill.getId(), new SkillEntry(skill.getMaxLevel(), skill.getMaxLevel(), -1));
        }
        target.changeMapInternal(target.getMap(), target.getPosition(), MaplePacketCreator.getCharInfo(target));
    }

    private void Heal(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (cmd.getName().endsWith("m")) {
            for (MapleCharacter players : player.getMap().getCharacters()) {
                players.setHp(Math.max(players.getMaxHp(), players.getCurrentMaxHp()));
                players.setMp(Math.max(players.getMaxMp(), players.getCurrentMaxMp()));
                players.updateSingleStat(MapleStat.HP, players.getHp());
                players.updateSingleStat(MapleStat.MP, players.getMp());
            }
        } else if (args.length() > 0) {
            for (int i = 0; i < args.length(); i++) {
                String username = args.get(i);
                MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
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
    }

    private void Notice(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            String message = args.concatFrom(0);
            player.getClient().getWorldServer().sendMessage(0, message);
        } else {
            player.dropMessage(5, "You must specify a message");
        }
    }

    private void Gift(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

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
    }

    private void Kill(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            for (int i = 0; i < args.length(); i++) {
                String username = args.get(i);
                MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    target.setHpMp(0);
                }
            }
        } else {
            player.sendMessage(5, "usage: !{} <usernames...>", cmd.getName());
        }
    }

    private void KillMap(MapleCharacter player, Command cmd, CommandArgs args) {
        for (MapleCharacter players : player.getMap().getCharacters()) {
            if (!players.isGM() || players.isDebug()) {
                players.setHpMp(0);
            }
        }
    }

    private void ReloadMap(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() == 0) {
            for (MapleChannel channel : world.getChannels()) {
                channel.reloadMap(player.getMapId());
            }
        } else if (args.length() == 1) {
            Integer mapId = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target != null) {
                    mapId = target.getMapId();
                } else {
                    return;
                }
            }
            for (MapleChannel channel : world.getChannels()) {
                channel.reloadMap(mapId);
            }
            player.sendMessage(6, "Reloaded map {}", mapId);
        } else {
            player.sendMessage(5, "Syntax: ![] <map ID>", cmd.getName());
        }
    }

    private void KillAll(MapleCharacter player, Command cmd, CommandArgs args) {
        player.getMap().killAllMonsters();
        player.dropMessage("Killed all monsters!");
    }

    private void ClearDrops(MapleCharacter player, Command cmd, CommandArgs args) {
        player.getMap().clearDrops(player);
        player.dropMessage("Cleared all drops");
    }

    private void ClearInventory(MapleCharacter player, Command cmd, CommandArgs args) {
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
                            try (Connection con = player.getClient().getWorldServer().getConnection()) {
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
                    MapleInventoryManipulator.removeById(player.getClient(), iType, itemId, quantity, false, false);
                }
            }
        } else {
            player.sendMessage(5, "Syntax: !{} <inventory>", cmd.getName());
        }
    }

    private void SetHide(MapleCharacter player, Command cmd, CommandArgs args) {
        Integer number = player.getGMLevel();
        if (args.length() == 1) {
            number = args.parseNumber(0, int.class);
            if (number == null) {
                player.sendMessage(5, "{} is not a number", args.get(0));
                return;
            } else if (number < 1 || number > player.getGMLevel()) {
                player.sendMessage(5, "You must enter a value btween 1 and {}", player.getGMLevel());
                return;
            }
        }
        player.setHidingLevel(number);
        player.toggleHidden(!player.isHidden());
        player.sendMessage(6, "You have entered hide level {}", number);
    }

    private void MaxStats(MapleCharacter player, Command cmd, CommandArgs args) {
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
    }

    private void Dispel(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            if (args.length() == 1 && args.get(0).equalsIgnoreCase("map")) {
                player.getMap().getCharacters().forEach(MapleCharacter::dispelDebuffs);
            } else {
                for (int i = 0; i < args.length(); i++) {
                    String username = args.get(i);
                    MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                    if (target != null) {
                        target.dispelDebuffs();
                    }
                }
                player.dropMessage(6, "Done!");
            }
        } else {
            player.dropMessage(5, "Syntax: !debuff <usernames/map>");
        }
    }


    private void MultiUser(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            HashMap<String, String> HWIDs = new HashMap<String, String>();

            for (MapleChannel channel : player.getClient().getWorldServer().getChannels()) {
                StringBuilder sb = new StringBuilder();
                Collection<MapleCharacter> players = channel.getPlayers();
                for (MapleCharacter online : players) {
                    if (!HWIDs.containsKey(online.getClient().getHWID())) {
                        HWIDs.put(online.getClient().getHWID(), online.getName());
                    } else {
                        sb.append(online.getName()).append(", ");
                        sb.append(HWIDs.get(online.getClient().getHWID())).append(", ");
                    }

                }

                player.sendMessage("{}", sb.toString());
                players.clear();
            }

            HWIDs.clear();

        }
    }

    private void Online(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            for (MapleChannel channel : player.getClient().getWorldServer().getChannels()) {
                Collection<MapleCharacter> players = channel.getPlayers();
                StringBuilder sb = new StringBuilder();
                for (MapleCharacter online : players) {
                    sb.append(online.getName()).append(", ");
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 2);
                }
                player.sendMessage("Channel {} - {} players", channel.getId(), players.size());
                players.clear();
                player.sendMessage("{}", sb.toString());
            }
        } else if (args.get(0).equalsIgnoreCase("npc")) {
            StringBuilder sb = new StringBuilder();
            for (MapleChannel channel : player.getClient().getWorldServer().getChannels()) {
                Collection<MapleCharacter> players = channel.getPlayers();
                sb.append("#echannel ").append(channel.getId()).append(" - ");
                StringBuilder usernames = new StringBuilder();
                for (MapleCharacter online : players) {
                    usernames.append(online.getName()).append(" ");
                }
                sb.append(players.size()).append(" players#n\r\n");
                players.clear();
                sb.append(usernames.toString());
                sb.append("\r\n");
                usernames.setLength(0);
            }
            player.getClient().announce(MaplePacketCreator.getNPCTalk(10200, (byte) 0, sb.toString(), "00 00", (byte) 0));
            sb.setLength(0);
        }
    }

    private void GMChat(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            String message = args.concatFrom(0);
            player.getClient().getWorldServer().sendMessageIf(p -> p.getGMLevel() > 0, 2, "[GM] {} : {}", player.getName(), message);
        } else {
            player.dropMessage(5, "You must specify a message");
        }
    }

    private void ItemVac(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleMap.doItemVac(player, null, -1);
    }

    private void Characters(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            String username = args.get(0);
            ArrayList<String> usernames = new ArrayList<>();
            // will this statement work? who knows
            try (Connection con = player.getClient().getWorldServer().getConnection();
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
    }

    private void Jail(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (cmd.getName().equalsIgnoreCase("unjail")) {
            if (args.length() == 1) {
                MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
                if (target != null) {
                    JailManager.removeJail(target.getId());
                    if (JailManager.isJailField(target.getMapId())) {
                        target.changeMap(ServerConstants.HOME_MAP);
                    }
                    target.sendMessage(6, "You have been release from jail by {}", player.getName());
                    player.sendMessage(6, "Success");
                } else {
                    player.sendMessage(5, "Unable to find any player named {}", args.get(0));
                }
            } else {
                player.sendMessage(5, "Syntax: !{} <username>", cmd.getName());
            }
        } else if (args.length() >= 2) {
            MapleCharacter target = world.findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target != null) {
                String reason = args.concatFrom(1);
                if (!reason.isEmpty()) {
                    JailManager.insertJail(target.getId(), player.getId(), reason);
                    target.changeMap(JailManager.getRandomField());
                    target.sendMessage(5, "You have been jailed by '{}'", player.getName());
                    world.sendMessage(6, "'{}' has been jailed for '{}'", target.getName(), reason);
                } else {
                    player.sendMessage(5, "You must provide a reason for your jail");
                }
            } else {
                player.sendMessage(5, "Unable to find player named '{}'", args.get(0));
            }
        } else if (args.length() == 1 && args.get(0).equalsIgnoreCase("logs")) {
            ArrayList<JailManager.JailLog> logs = JailManager.retrieveLogs();
            if (logs.isEmpty()) {
                player.sendMessage(6, "There are no players jailed");
            } else {
                for (JailManager.JailLog log : logs) {
                    String tUsername = MapleCharacter.getNameById(log.playerId);
                    String aUsername = MapleCharacter.getNameById(log.accuser);
                    player.sendMessage("'{}' jailed '{}' for '{}' on {}", aUsername, tUsername, log.reason, DateFormat.getDateTimeInstance().format(log.timestamp));
                }
            }
        } else {
            player.dropMessage(5, "Syntax: !jail <username> <reason>");
        }
    }


    private void Search(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 1) {
            String type = args.get(0);
            String search = args.concatFrom(1).toLowerCase().trim();
            MapleData data;
            MapleDataProvider dataProvider = MapleDataProviderFactory.getWZ(new File(System.getProperty("wzpath") + "/" + "String.wz"));
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
            player.message("Syntax: !search <type> <name> where type is map, item, or mob.");
        }
    }

    private void ChatType(MapleCharacter player, Command cmd, CommandArgs args) {
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
    }

    private void SetAP(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        Integer amount = args.parseNumber(0, int.class);
        if (amount == null) {
            player.dropMessage(5, args.getFirstError());
            return;
        }
        if (args.length() > 1) {
            for (int i = 1; i < args.length(); i++) {
                final String username = args.get(i);
                MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    target.setRemainingAp(amount);
                    target.updateSingleStat(MapleStat.AVAILABLEAP, amount);
                    target.sendMessage(6, "Your Ability Points has been updated to {}", target.getRemainingAp());
                }
            }
            player.dropMessage("Done!");
        } else {
            player.setRemainingAp(amount);
            player.updateSingleStat(MapleStat.AVAILABLEAP, amount);
            player.dropMessage("Done!");
        }
    }

    private void SetSP(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        Integer amount = args.parseNumber(0, int.class);
        if (amount == null) {
            player.dropMessage(5, args.getFirstError());
            return;
        }
        if (args.length() > 1) {
            for (int i = 1; i < args.length(); i++) {
                final String username = args.get(i);
                MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                if (target != null) {
                    target.setRemainingSp(amount);
                    target.updateSingleStat(MapleStat.AVAILABLESP, amount);
                    target.sendMessage(6, "Your Skill Points has been updated to {}", target.getRemainingAp());
                }
            }
            player.dropMessage("Done!");
        } else {
            player.setRemainingSp(amount);
            player.updateSingleStat(MapleStat.AVAILABLESP, amount);
            player.dropMessage("Done!");
        }
    }

    private static final int[] SkillBufs = {
            1001003, 2001002, 1101006, 1101007, 1301007, 2201001, 2121004, 2111005, 2311003, 1121002, 4211005, 3121002,
            1121000, 2311003, 1101004, 1101006, 4101004, 4111001, 2111005, 1111002, 2321005, 3201002, 4101003, 4201002,
            5101006, 1321010, 1121002, 1120003};

    private void Buff(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        MapleCharacter target = player;
        if (args.length() == 1) {
            target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target == null) {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                return;
            }
        }
        for (int skill : SkillBufs) {
            Skill s = SkillFactory.getSkill(skill);
            if (s != null) {
                s.getEffect(s.getMaxLevel()).applyTo(target);
            }
        }
    }

    private void SetFame(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();

        if (args.length() > 0) {
            Integer amount = args.parseNumber(0, int.class);
            if (amount == null) {
                player.sendMessage(5, args.getFirstError());
            } else if (amount < 0 || amount > 32767) {
                player.sendMessage(5, "You cannot give that much fame");
            } else if (args.length() > 1) {
                for (int i = 1; i < args.length(); i++) {
                    String username = args.get(i);
                    MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
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
    }

    private void SetMesos(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
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
                    MapleCharacter chr = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
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
    }

    private void SetGender(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() == 2) {
            MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
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
    }

    private void SetStats(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() > 0) {
            Integer stat = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            MapleCharacter target = player;
            if (args.length() == 2) {
                target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(1)));
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

    private void SetOccupation(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() != 1) {
            player.dropMessage("Syntax: !occupation <occupation_name>");
            Occupation.Type[] values = Occupation.Type.values();
            StringBuilder sb = new StringBuilder();
            for (Occupation.Type value : values) {
                sb.append(value.name().toLowerCase()).append("(").append(value.ordinal()).append(")").append(", ");
            }
            sb.setLength(sb.length() - 2);
            player.sendMessage(sb.toString());
            return;
        }
        Number number = args.parseNumber(0, int.class);
        if (number == null) {
            if (args.get(0).equals("none")) {
                player.setOccupation(null);
                player.sendMessage("Occupation has been removed");
            } else {
                player.sendMessage(args.getFirstError());
            }
            return;
        }
        Occupation.Type type = Occupation.Type.fromValue(number.intValue());
        if (type != null) {
            player.setOccupation(new Occupation(type));
            player.setRates();
            player.sendMessage("Occupation has changed to {}", type.name());
        } else {
            player.sendMessage("{} is not a valid occupation", number.intValue());
        }
    }
}