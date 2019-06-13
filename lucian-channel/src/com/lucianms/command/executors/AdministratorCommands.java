package com.lucianms.command.executors;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleRing;
import com.lucianms.client.Relationship;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.Equip;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.events.PlayerRingActionEvent;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.features.scheduled.SAutoEvent;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventManager;
import com.lucianms.io.scripting.map.FieldScriptExecutor;
import com.lucianms.io.scripting.portal.PortalScriptManager;
import com.lucianms.io.scripting.reactor.ReactorScriptManager;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleShopFactory;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.*;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleReactor;
import com.lucianms.server.maps.PlayerNPC;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

import javax.script.ScriptException;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

/**
 * Level 6 permission requirement
 *
 * @author izarooni
 */
public class AdministratorCommands extends CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministratorCommands.class);
    private static ArrayList<String> HELP_LIST;

    public AdministratorCommands() {
        addCommand("admincmds", this::CommandList, "List of administrator commands");
        addCommand("clearcache", this::CommandClearCache, "Clear cache of a specified data-set");
//        addCommand("forceevent", this::CommandForceEvent, "Force proc of an auto-event");
        addCommand("forceautoevent", this::CommandAutoForceEvent, "Force proc of a scheduled auto-event");
        addCommand("list", this::CommandDebugList, "List of all entities of a specific type");
        addCommand("reloadevent", this::CommandReloadEvent, "Reload an event script");
        addCommand("reloadevents", this::CommandReloadEvents, "Reload all event scripts");
        addCommand("wpos", this::CommandWarpPosition, "Warp to a position in the current map");
        addCommand("setgmlevel", this::CommandSetGMLevel, "Change the GM level of a specified player");
        addCommand("setcouple", this::CommandSetCouple, "Create a couple ring with 2 specified players");
        addCommand("tasks", this::CommandShowTasks, "View information on the thread pool executor");
        addCommand("fakeplayer", this::CommandCreateClone, "Create a clone of your player");
        addCommand("pb", this::CommandBakePacket, "Create a packet via hexadecimal characters");
        addCommand("wipe", this::CommandWipePlayer, "Clear all items for a specified player");
        addCommand("test", this::CommandTest, "Test command");
        addCommand("stop", this::CommandExit, "Stop the server after a specified amount of time (in seconds)");

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

    private void DoShutDownServer() {
        if (DiscordConnection.getSession() != null) {
            MaplePacketWriter w = new MaplePacketWriter();
            w.write(Headers.Shutdown.value);
            DiscordConnection.sendPacket(w.getPacket());
        }
        ConsoleCommands.getInstance().stopReading();
        System.exit(0);
    }

    private void CommandExit(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            player.sendMessage("Please specify the delay (in seconds) that the server should wait before shutting down");
        } else {
            Number nTime = args.parseNumber(0, int.class);
            if (nTime == null) {
                player.sendMessage("{} is not a number", args.get(0));
                return;
            } else if (nTime.intValue() > 180) {
                player.sendMessage("Please do not specify such a long delay");
                return;
            }
            long time = (nTime.intValue() + 1) * 1000;
            for (MapleWorld world : Server.getWorlds()) {
                world.sendPacket(MaplePacketCreator.serverMessage(String.format("The server will shutdown in %s. Please complete your tasks and log-out safely.", StringUtil.getTimeElapse(time))));
            }
            TaskExecutor.createTask(this::DoShutDownServer, time);
        }
    }

    private void CommandTest(MapleCharacter player, Command cmd, CommandArgs args) {
    }

    private void CommandWipePlayer(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() >= 1) {
            boolean wiped = MapleCharacter.wipe(args.get(0));
            if (wiped) {
                player.dropMessage(6, "Wipe of the player was successful");
            } else {
                player.dropMessage(5, "Wipe of player was unsuccessful");
            }
        }
    }

    private void CommandBakePacket(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 0) {
            player.sendMessage(5, "Not enough data provided");
            return;
        }
        player.announce(HexTool.getByteArrayFromHexString(args.concatFrom(0)));
    }

    private void CommandReloadEvents(MapleCharacter player, Command cmd, CommandArgs args) {
        for (MapleWorld worlds : Server.getWorlds()) {
            for (MapleChannel channels : worlds.getChannels()) {
                channels.reloadEventScriptManager();
            }
        }
        player.dropMessage(6, "Event script reloaded");
    }

    private void CommandCreateClone(MapleCharacter player, Command cmd, CommandArgs args) {
        if (player.getFakePlayer() == null) {
            FakePlayer fake = new FakePlayer(player.getName() + "'s Toy");
            fake.setMap(player.getMap());
            fake.clonePlayer(player);
            player.setFakePlayer(fake);
            player.getMap().addFakePlayer(fake);
            fake.setFollowing(true);
        } else {
            player.sendMessage("You already have a fake player");
        }
    }

    private void CommandShowTasks(MapleCharacter player, Command cmd, CommandArgs args) {
        ScheduledThreadPoolExecutor exe = TaskExecutor.getExecutor();
        player.sendMessage("{} Tasks", exe.getTaskCount());
        player.sendMessage("{} in queue", exe.getQueue().size());
        player.sendMessage("{} are active", exe.getActiveCount());
        player.sendMessage("{} is the largest pool size", exe.getLargestPoolSize());
        player.sendMessage("{} is the maximum pool size", exe.getMaximumPoolSize());
    }

    private void CommandSetCouple(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 3) {
            Integer engagementBoxID = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.sendMessage(5, error);
                return;
            } else if (engagementBoxID != 2240000 && engagementBoxID != 2240001 && engagementBoxID != 2240002 && engagementBoxID != 2240003) {
                player.sendMessage(5, "Invalid engagement box ID");
                return;
            }
            MapleWorld world = player.getClient().getWorldServer();
            MapleCharacter target1 = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(1)));
            MapleCharacter target2 = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(2)));
            if (target1 == null) {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(2));
                return;
            } else if (target1.getRelationship().getStatus() != Relationship.Status.Single) {
                player.sendMessage(5, "The player '{}' is not single!", target1.getName());
                return;
            } else if (!MapleInventoryManipulator.checkSpace(target1.getClient(), engagementBoxID, 1, "")) {
                player.sendMessage(5, "The player '{}' has a full inventory", target1.getName());
                return;
            }
            if (target2 == null) {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(1));
                return;
            } else if (target2.getRelationship().getStatus() != Relationship.Status.Single) {
                player.sendMessage(5, "The player '{}' is not single!", target2.getName());
                return;
            } else if (!MapleInventoryManipulator.checkSpace(target2.getClient(), engagementBoxID, 1, "")) {
                player.sendMessage(5, "The player '{}' has a full inventory", target2.getName());
                return;
            }

            Relationship rltn = target1.getRelationship();
            Relationship prltn = target2.getRelationship();

            final int ringItemID = PlayerRingActionEvent.getWeddingRingForEngagementBox(engagementBoxID);
            int ringID;
            try {
                ringID = MapleRing.create(ringItemID, target1, target2);
            } catch (Exception e) {
                LOGGER.error("Failed to create ring between {} and {}", target1.getName(), target2.getName(), e);
                player.sendMessage(1, "Failed to create the ring.");
                return;
            }

            MapleRing t1Ring = new MapleRing(ringID, ringID + 1, target2.getId(), ringItemID, target2.getName());
            MapleRing t2Ring = new MapleRing(ringID + 1, ringID, player.getId(), ringItemID, player.getName());

            Equip equip = new Equip(ringItemID, (short) 0);
            equip.setRingId(ringID);
            MapleInventoryManipulator.addFromDrop(target1.getClient(), equip, true);
            rltn.setStatus(Relationship.Status.Married);
            rltn.setEngagementBoxId(engagementBoxID);
            rltn.setBrideId(target2.getId());
            rltn.setGroomId(target1.getId());
            target1.getWeddingRings().add(t1Ring);

            equip = new Equip(ringItemID, (short) 0);
            equip.setRingId(ringID + 1);
            MapleInventoryManipulator.addFromDrop(target2.getClient(), equip, true);
            prltn.setStatus(Relationship.Status.Married);
            prltn.setEngagementBoxId(engagementBoxID);
            prltn.setBrideId(target2.getId());
            prltn.setGroomId(target1.getId());
            target2.getWeddingRings().add(t2Ring);

            target1.changeMapInternal(target1.getMap(), target1.getPosition(), MaplePacketCreator.getCharInfo(target1));
            target2.changeMapInternal(target2.getMap(), target2.getPosition(), MaplePacketCreator.getCharInfo(target2));

            target1.announce(PlayerRingActionEvent.getEngagementSuccess(target1, target2));
            target2.announce(PlayerRingActionEvent.getEngagementSuccess(target1, target2));
            target1.sendMessage(6, "You are now married to '{}'", target2.getName());
            target2.sendMessage(6, "You are now married to '{}'", target1.getName());
            player.sendMessage(6, "Success!");
        } else {
            player.sendMessage(5, "usage: !setcouple <engagement_box> <groom> <bride>");
        }
    }

    private void CommandSetGMLevel(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 2) {
            Integer GMLevel = args.parseNumber(1, int.class);
            if (GMLevel == null) {
                player.sendMessage(5, args.getFirstError());
                return;
            }
            MapleWorld world = player.getClient().getWorldServer();
            MapleCharacter target = world.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
            if (target != null) {
                target.setGM(GMLevel);
                target.sendMessage(6, "Your GM level has been updated");
                player.sendMessage(6, "Success!");
            } else {
                player.sendMessage(5, "Unable to find any player named '{}'", args.get(0));
            }
        } else {
            player.sendMessage(5, "usage: !setgmlevel <username> <gm_level>");
        }
    }

    private void CommandWarpPosition(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 2) {
            Integer x = args.parseNumber(0, int.class);
            Integer y = args.parseNumber(1, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            player.changeMap(player.getMap(), new Point(x, y));
        }
    }

    private void CommandReloadEvent(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            String scriptName = args.get(0);
            MapleWorld world = player.getClient().getWorldServer();
            for (MapleChannel channel : world.getChannels()) {
                EventManager manager = channel.getEventScriptManager().getManager(scriptName);
                if (manager == null) {
                    player.sendMessage(5, "Unable to find any event named '{}'", scriptName);
                    return;
                }
                manager.cancel();
                channel.getEventScriptManager().putManager(scriptName);
                try {
                    EventManager em = channel.getEventScriptManager().getManager(scriptName);
                    try {
                        em.getInvocable().invokeFunction("init", (Object) null);
                    } catch (ScriptException | NoSuchMethodException e) {
                        e.printStackTrace();
                        player.dropMessage("An error occurred");
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    player.dropMessage("Unable to restart event due to an error");
                }
            }
            player.dropMessage("Done!");
        }
    }

    private void CommandDebugList(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            Function<Point, String> readable = p -> String.format("X:%d, Y:%d", p.x, p.y);
            if (args.get(0).equalsIgnoreCase("reactors")) {
                player.sendMessage("Listing reactors...");
                for (MapleMapObject object : player.getMap().getReactors()) {
                    MapleReactor reactor = (MapleReactor) object;
                    player.sendMessage("{} / id:{} / oid:{} / name:{}", readable.apply(reactor.getPosition()), reactor.getId(), reactor.getObjectId(), reactor.getName());
                }
            } else if (args.get(0).equalsIgnoreCase("monsters")) {
                player.sendMessage("Listing monsters...");
                for (MapleMonster monsters : player.getMap().getMonsters()) {
                    player.sendMessage("{} / id:{} / oid:{} / name:{} / HP:{} / level:{}", readable.apply(monsters.getPosition()), monsters.getId(), monsters.getObjectId(), monsters.getName(), monsters.getHp(), monsters.getLevel());
                }
            } else if (args.get(0).equalsIgnoreCase("npcs")) {
                player.sendMessage("Listing npcs...");
                for (MapleMapObject object : player.getMap().getMapObjects()) {
                    if (object instanceof MapleNPC) {
                        MapleNPC npc = ((MapleNPC) object);
                        player.sendMessage("{} / id:{} / oid:{} / name:{} / script:{}", readable.apply(npc.getPosition()), npc.getId(), npc.getObjectId(), npc.getName(), npc.getScript());
                    } else if (object instanceof PlayerNPC) {
                        PlayerNPC npc = ((PlayerNPC) object);
                        player.sendMessage("{} / id:{} / oid:{} / name:{} / script:{}", readable.apply(npc.getPosition()), npc.getId(), npc.getObjectId(), npc.getName(), npc.getScript());
                    }
                }
            } else {
                player.sendMessage("Available types: reactors, monsters, npcs");
            }
        }
    }

    private void CommandForceEvent(MapleCharacter player, Command cmd, CommandArgs args) {
        GAutoEventManager[] manager = GAutoEventManager.values();
        if (args.length() == 1) {
            Integer index = args.parseNumber(0, int.class);
            String error = args.getFirstError();
            if (error != null) {
                player.dropMessage(5, error);
                return;
            }
            if (index < 0 || index >= manager.length) {
                player.dropMessage("You must pick a number between 1 and " + manager.length);
                return;
            }
            GAutoEventManager event = manager[index];
            if (GAutoEventManager.getCurrentEvent() != null) {
                GAutoEventManager.getCurrentEvent().stop();
            }
            try {
                GAutoEvent gEvent = event.getClazz().getDeclaredConstructor(MapleWorld.class).newInstance(player.getClient().getWorldServer());
                gEvent.start();
                GAutoEventManager.setCurrentEvent(gEvent);
                player.dropMessage("Success!");
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                player.dropMessage("An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            player.dropMessage(5, "Not enough arguments. !fae <event_id>");
            for (GAutoEventManager manage : manager) {
                player.dropMessage(String.format("%d - %s", (manage.ordinal() + 1), manage.getName()));
            }
        }
    }

    private void CommandAutoForceEvent(MapleCharacter player, Command cmd, CommandArgs args) {
        MapleWorld world = player.getClient().getWorldServer();
        if (args.length() == 1) {
            SAutoEvent scheduledEvent = world.getScheduledEvent(args.get(0));
            scheduledEvent.run();
        } else {
            player.sendMessage(5, "Usage: !{} <event_name>", cmd.getName());
        }
    }

    private void CommandClearCache(MapleCharacter player, Command cmd, CommandArgs args) {
        if (args.length() == 1) {
            switch (args.get(0)) {
                default:
                    return;
                case "achievements":
                    Achievements.loadAchievements();
                    player.sendMessage("Achievement scripts reloaded");
                    break;
                case "skills":
                    SkillFactory.createCache();
                    player.sendMessage("Skill data reloaded");
                    break;
                case "shops":
                    MapleShopFactory.getInstance().clearCache();
                    player.sendMessage("NPC shops cache cleared");
                    break;
                case "items":
                    MapleItemInformationProvider.getInstance().clearCache();
                    player.sendMessage("Item cache cleared");
                    break;
                case "monsters":
                    MapleLifeFactory.clearCache();
                    player.sendMessage("Monster cache cleared");
                    break;
                case "portals":
                    PortalScriptManager.clearPortalScripts();
                    player.sendMessage("Portal scripts cache cleared");
                    break;
                case "drops":
                    ReactorScriptManager.clearDrops();
                    MapleMonsterInformationProvider.clearCache();
                    player.sendMessage("Drop data cache reloaded");
                    break;
                case "mapscripts":
                    FieldScriptExecutor.clearCache();
                    player.sendMessage("Map scripts cache cleared");
                    break;
                case "cquests":
                    CQuestBuilder.loadAllQuests();
                    player.sendMessage("Custom quests reloaded");
                    break;
            }
            System.gc();
        } else {
            player.sendMessage("achievements, shops, items, monsters, drops, mapscripts, cquests");
        }
    }
}
