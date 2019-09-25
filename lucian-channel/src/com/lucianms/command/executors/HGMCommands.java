package com.lucianms.command.executors;

import com.lucianms.client.*;
import com.lucianms.client.inventory.*;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import com.lucianms.command.CommandWorker;
import com.lucianms.constants.ItemConstants;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.server.*;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.life.*;
import com.lucianms.server.maps.MapleFoothold;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Pair;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

/**
 * @author izarooni
 * @author lucasdieswagger
 */
public class HGMCommands extends CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HGMCommands.class);
    private static ArrayList<String> HELP_LIST;

    public HGMCommands() {
        addCommand("hgmcmds", this::CommandList, "");
        addCommand("guildrank", this::SetGuildRank, "");
        addCommand("popup", this::PopupNotice, "");
        addCommand("setname", this::SetName, "");
        addCommand("clearskills", this::ClearSkills, "");
        addCommand("hpmp", this::SetHpMp, "");
        addCommand("debug", this::ToggleDebugger, "");
        addCommand("respawn", this::RespawnMap, "");
        addCommand("npc", this::SpawnNpc, "");
        addCommand("pnpc", this::SpawnNpc, "");
        addCommand("pmob", this::SpawnPermMob, "");
        addCommand("playernpc", this::SpawnPlayerNpc, "");
        addCommand("pos", this::MyLocation, "");

        addCommand("shout", this::WorldSpeak, "");
        addCommand("whereami", this::ImFuckingLost, "");
        addCommand("oshop", this::OpenShop, "");
        addCommand("onpc", this::OpenNpc, "");
        addCommand("saveall", this::SaveWorld, "");
        addCommand("resetreactors", this::ResetReactors, "");
        addCommand("sudo", this::IAmRoot, "");
        addCommand("stalker", this::OpenStalkerNpc, "");
        addCommand("godmeup", this::BoostMyEquips, "");
        addCommand("footholds", this::ListFootholds, "");
        addCommand("ring", this::CreateRing, "");

        Map<String, Pair<CommandEvent, String>> commands = getCommands();
        HELP_LIST = new ArrayList<>(commands.size());
        for (Map.Entry<String, Pair<CommandEvent, String>> e : commands.entrySet()) {
            HELP_LIST.add(String.format("!%s - %s", e.getKey(), e.getValue().getRight()));
        }
        HELP_LIST.sort(String::compareTo);
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

    private void SetGuildRank(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 2) {
            String username = args.get(0);
            Integer newRank = args.parseNumber(1, int.class);
            if (newRank == null) {
                player.sendMessage(args.getFirstError());
                return;
            } else if (newRank < 1 || newRank > 5) {
                player.sendMessage(5, "Guild rank must be between 1 (master) and 5 (lowest member)");
                return;
            }
            MapleGuild guild = player.getGuild();
            if (guild != null) {
                for (MapleGuildCharacter member : guild.getMembers()) {
                    if (member.getName().equalsIgnoreCase(username)) {
                        guild.changeRank(member.getId(), newRank);
                        break;
                    }
                }
            } else {
                player.sendMessage(5, "You must be in a guild!");
            }
        } else {
            player.sendMessage(5, "syntax: !{} <username> <new rank>", cmd.getName());
        }
    }

    private void PopupNotice(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            String content = args.concatFrom(0);
            player.getClient().getWorldServer().sendMessage(1, content);
        } else {
            player.sendMessage(5, "You must type a message!");
        }
    }

    private void SetName(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 2) {
            MapleCharacter target = player.getClient().getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target != null) {
                String oldName = target.getName();
                target.setName(args.get(1));
                target.sendMessage(6, "Your name has been changed to '{}'", target.getName());
                player.sendMessage(6, "Changed '{}''s name to '{}'. Relog to make changes", oldName, target.getName());
            } else {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
            }
        } else if (args.length() == 1) {
            player.setName(args.get(0));
            player.sendMessage(6, "Your name has been changed to '{}'. Relog to make changes", player.getName());
        } else {
            player.sendMessage(5, "Usage: !setname <username> - Change your own username");
            player.sendMessage(5, "Usage: !setname <target> <username> - Change another player's username");
        }
    }

    private void ClearSkills(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleCharacter target;
        if (args.length() == 1) {
            target = player.getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target == null) {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                return;
            }
        } else if (args.length() > 1) {
            player.sendMessage("One username at a time please!");
            return;
        } else {
            target = player;
        }
//        int totalSP = 0;
        for (Map.Entry<Integer, SkillEntry> entry : new HashMap<>(target.getSkills()).entrySet()) {
            Integer skillID = entry.getKey();
            SkillEntry value = entry.getValue();
            Skill skill = SkillFactory.getSkill(skillID);
//            totalSP += value.getLevel();
            target.changeSkillLevel(entry.getKey(), (byte) (skill.isHidden() ? -1 : 0), skill.isHidden() ? 0 : value.getMasterLevel(), value.getExpiration());
        }
//        target.setRemainingSp(totalSP);
//        target.updateSingleStat(MapleStat.AVAILABLESP, totalSP);
    }

    private void SetHpMp(MapleCharacter player, Command cmd, CommandArgs args) {
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
        } else {
            player.sendMessage(5, "Usage: !{} <hp> <mp>", cmd.getName());
        }
    }

    private void ToggleDebugger(MapleCharacter player, Command cmd, CommandArgs args) {
        player.setDebug(!player.isDebug());
        player.sendMessage("Your debug mode is now {}", (player.isDebug() ? "enabled" : "disabled"));
    }



    private void RespawnMap(MapleCharacter player, Command cmd, CommandArgs args) {
        for (SpawnPoint sp : player.getMap().getMonsterSpawnPoints()) {
            if (sp.canSpawn(true)) {
                sp.summonMonster();
            }
        }
        player.getMap().resetReactors();
        player.sendMessage("Monsters and reactors have respawned and reset");
    }


    private void SpawnNpc(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            MapleWorld world = player.getClient().getWorldServer();

            boolean permanent = cmd.equals("pnpc");
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
            for (MapleChannel channel : world.getChannels()) {
                MapleMap iMap = channel.getMap(player.getMapId());
                if (iMap != null) {
                    iMap.addMapObject(npc);
                    iMap.broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                }
            }
            if (permanent) {
                try (Connection con = world.getConnection();
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
    }

    private void SpawnPermMob(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 0) {
            MapleMap map = player.getMap();

            Integer mobId = args.parseNumber(0, int.class);
            Integer amount = args.parseNumber(1, 1, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = map.getFootholds().findBelow(player.getPosition()).getId();
            for (int i = 0; i < amount; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mobId);
                if (mob != null) {
                    mob.setPosition(player.getPosition());
                    mob.setCy(ypos);
                    mob.setRx0(xpos + 50);
                    mob.setRx1(xpos - 50);
                    mob.setFh(fh);
                    try (Connection con = player.getClient().getWorldServer().getConnection();
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
                    map.addMonsterSpawn(mob, 0, -1);
                } else {
                    player.dropMessage(5, String.format("'%s' is an invalid monster", args.get(0)));
                    return;
                }
            }
        } else {
            player.dropMessage(5, "You must specify a monster ID");
        }
    }

    private void SpawnPlayerNpc(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 1) {
            Integer npcId = args.parseNumber(0, int.class);
            String username = args.get(1);
            String script = (args.length() == 3) ? args.get(2) : null;
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            MapleCharacter target = player.getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
            if (target != null) {
                if (npcId >= 9901000 && npcId <= 9901909) {
                    player.createPlayerNPC(target, npcId, script);
                } else {
                    player.dropMessage(5, "Player NPCs ID must be between 9901000 and 9901909");
                }
            } else {
                player.dropMessage(5, String.format("Unable to find any player named '%s'", username));
            }
        } else {
            player.dropMessage(5, "Syntax: !playernpc <npc_id> <username> [script_name]");
        }
    }

    private void MyLocation(MapleCharacter player, Command cmd, CommandArgs args) {
        player.dropMessage(player.getPosition().toString());
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

    private void ImFuckingLost(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleMap map = player.getMap();

        player.sendMessage(6, "ID: {}", player.getMapId());
        player.sendMessage(6, "Name: {} - {}", map.getStreetName(), map.getMapName());
        player.sendMessage(6, "onUserEnter: {} ", map.getOnUserEnter());
        player.sendMessage(6, "onFirstUserEnter: {} ", map.getOnFirstUserEnter());
        player.sendMessage(6, "Everlasting drops: {}", map.getEverlast());
        player.sendMessage(6, "Capacity: {} / Spawned: {}", map.getMobCapacity(), map.getSpawnedMonstersOnMap().get());
    }

    private void OpenShop(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            Integer shopId = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            MapleShop shop = MapleShopFactory.getInstance().getShop(shopId);
            if (shop != null) {
                shop.sendShop(player.getClient());
            } else {
                player.dropMessage(5, "This shop doesn't exist");
            }
        }
    }

    private void OpenNpc(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            Integer npcId = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error == null && npcId < 9901000) {
                NPCScriptManager.start(player.getClient(), npcId);
            } else {
                String script = args.get(0);
                if (script.startsWith("`")) {
                    script = script.substring(1);
                }
                NPCScriptManager.start(player.getClient(), 10200, script);
            }
        } else {
            player.sendMessage(5, "Usage: !{} <npc_id/script>", cmd.getName());
        }
    }

    private void SaveWorld(MapleCharacter player, Command cmd, CommandArgs args) {
        for (MapleWorld worlds : Server.getWorlds()) {
            Collection<MapleCharacter> players = worlds.getPlayers();
            players.forEach(MapleCharacter::saveToDB);
            players.clear();
        }
        player.dropMessage(6, "All characters saved!");
    }

    private void ResetReactors(MapleCharacter player, Command cmd, CommandArgs args) {
        player.getMap().resetReactors();
        player.dropMessage("Reactors reset");
    }

    private void IAmRoot(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() > 1) {
            MapleCharacter target = player.getClient().getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target != null) {
                String s = args.concatFrom(1);
                CommandWorker.process(target.getClient(), s, true);
            } else {
                player.sendMessage("Unable to find any player named '{}'", args.get(0));
            }
        }
    }

    private void OpenStalkerNpc(MapleCharacter player, Command cmd, CommandArgs args) {
        NPCScriptManager.start(player.getClient(), 10200, "t_stalker");

    }

    private void BoostMyEquips(MapleCharacter player, Command cmd, CommandArgs args) {
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
                        eq.setSpeed(stat);
                        eq.setJump(stat);
                    }
                    mods.add(new ModifyInventory(3, eq));
                    mods.add(new ModifyInventory(0, eq));
                }
            }
            player.getClient().announce(MaplePacketCreator.modifyInventory(true, mods));
            player.equipChanged(true);
            mods.clear();
        }
    }

    private void ListFootholds(MapleCharacter player, Command cmd, CommandArgs args) {
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

    private void CreateRing(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 2) {
            MapleCharacter partner = player.getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (partner == null) {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
                return;
            }
            Integer ringItemID = args.parseNumber(1, int.class);
            int ringID;
            try {
                ringID = MapleRing.create(ringItemID, player, partner);
            } catch (SQLException e) {
                player.sendMessage(5, "Failed to create ring");
                LOGGER.error("Failed to create ring between {} and {}", player.getName(), partner.getName(), e);
                return;
            }
            Equip equip = new Equip(ringItemID);
            equip.setRingId(ringID);
            MapleInventoryManipulator.addFromDrop(player.getClient(), equip, true);

            equip = new Equip(ringItemID);
            equip.setRingId(ringID + 1);
            MapleInventoryManipulator.addFromDrop(partner.getClient(), equip, true);
        } else {
            player.sendMessage(5, "usage: !ring <partner> <itemid>");
        }
    }
}