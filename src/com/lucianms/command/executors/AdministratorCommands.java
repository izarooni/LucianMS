package com.lucianms.command.executors;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleRing;
import client.Relationship;
import client.inventory.Equip;
import com.lucianms.command.CommandWorker;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.io.scripting.event.EventManager;
import com.lucianms.io.scripting.map.MapScriptManager;
import net.server.Server;
import net.server.channel.Channel;
import net.server.channel.handlers.RingActionHandler;
import net.server.world.World;
import server.MapleInventoryManipulator;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;
import server.maps.PlayerNPC;
import tools.HexTool;

import javax.script.ScriptException;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Level 6 permission requirement
 *
 * @author izarooni
 */
public class AdministratorCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        MapleCharacter player = client.getPlayer();
        World world = client.getWorldServer();
        Channel ch = client.getChannelServer();

        if (command.equals("admincommands")) {
            ArrayList<String> commands = new ArrayList<>();
            try {
                commands.add("!reloaddrops - Clear monster drop cache");
                commands.add("!reloadmapscripts - Clears stored map scripts");
                commands.add("!reloadquests - Reload custom quest");
                commands.add("!reloadachievements - Reload achievement scripts");
                commands.add("!fae - Force an auto event execution");
                commands.add("!list - Debug command");
                commands.add("!reloadevents - Reload all event scripts");
                commands.add("!reloadevent - Reload a specified event script");
                commands.add("!wpos - Warp yourself to a specified {x,y} position in the current map");
                commands.add("!setgmlevel - Change the GM level of a specified player");
                commands.add("!setcouple - Declare two specified players as a married couple");
                commands.sort(String::compareTo);
                commands.forEach(player::dropMessage);
            } finally {
                commands.clear();
            }
        } else if (command.equals("autoevent")) {
            if (args.length() == 1) {
                try {
                    GAutoEventManager auto = GAutoEventManager.valueOf(args.get(0));
                    auto.startInstance(world);
                } catch (IllegalArgumentException e) {
                    if (args.get(0).equals("stop")) {
                        GAutoEventManager.getCurrentEvent().stop();
                    } else {
                        player.sendMessage(5, "Unable to find any auto event named '{}'", args.get(0));
                    }
                }
            } else {
                player.sendMessage(5, "Usage: !autoevent <event_name>");
            }
        } else if (command.equals("reloaddrops")) {
            MapleMonsterInformationProvider.getInstance().reload();
            player.dropMessage("Drops reloaded");
        } else if (command.equals("reloadmapscripts")) {
            MapScriptManager.getInstance().clearScripts();
            player.dropMessage("Map scripts cleared");
        } else if (command.equals("reloadquests")) {
            CQuestBuilder.loadAllQuests();
            player.dropMessage("Quests reloaded");
        } else if (command.equals("reloadachievements")) {
            Achievements.loadAchievements();
            player.dropMessage("Achievements reloaded");
        } else if (command.equals("fae")) { // force auto event
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
                    GAutoEvent gEvent = event.getClazz().getDeclaredConstructor(World.class).newInstance(client.getWorldServer());
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
        } else if (command.equals("list")) {
            if (args.length() == 1) {
                if (args.get(0).equalsIgnoreCase("reactors")) {
                    for (MapleMapObject object : player.getMap().getReactors()) {
                        MapleReactor reactor = (MapleReactor) object;
                        player.sendMessage("{} / id:{} / oid:{} / name:{}", reactor.getPosition().toString(), reactor.getId(), reactor.getObjectId(), reactor.getName());
                    }
                } else if (args.get(0).equalsIgnoreCase("monsters")) {
                    for (MapleMonster monsters : player.getMap().getMonsters()) {
                        player.sendMessage("{} / id:{} / oid:{} / name:{} / HP:{}", monsters.getPosition().toString(), monsters.getId(), monsters.getObjectId(), monsters.getName(), monsters.getHp());
                    }
                } else if (args.get(0).equalsIgnoreCase("npcs")) {
                    for (MapleMapObject object : player.getMap().getMapObjects()) {
                        if (object instanceof MapleNPC) {
                            MapleNPC npc = ((MapleNPC) object);
                            player.sendMessage("{} / id:{} / oid:{} / name:{} / script:{}", npc.getPosition().toString(), npc.getId(), npc.getObjectId(), npc.getName(), npc.getScript());
                        } else if (object instanceof PlayerNPC) {
                            PlayerNPC npc = ((PlayerNPC) object);
                            player.sendMessage("{} / id:{} / oid:{} / name:{} / script:{}", npc.getPosition().toString(), npc.getId(), npc.getObjectId(), npc.getName(), npc.getScript());
                        }
                    }
                }
            }
        } else if (command.equals("wpos")) {
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
        } else if (command.equals("reloadevents")) {
            for (World worlds : Server.getInstance().getWorlds()) {
                for (Channel channels : worlds.getChannels()) {
                    channels.reloadEventScriptManager();
                }
            }
            player.dropMessage(6, "Event script reloaded");
        } else if (command.equals("reloadevent")) {
            if (args.length() == 1) {
                String scriptName = args.get(0);
                for (Channel channel : world.getChannels()) {
                    EventManager manager = channel.getEventScriptManager().getManager(scriptName);
                    if (manager == null) {
                        player.dropMessage(5, "Could not find any event named '" + scriptName + "'");
                        return;
                    }
                    manager.getInstances().forEach(EventInstanceManager::disbandParty);
                    channel.getEventScriptManager().putManager(scriptName);
                    try {
                        EventManager em = channel.getEventScriptManager().getManager(scriptName);
                        try {
                            em.getInvocable().invokeFunction("init", (Object) null);
                        } catch (ScriptException | NoSuchMethodException e) {
                            player.dropMessage("An error occurred");
                            e.printStackTrace();
                        }
                    } catch (RuntimeException e) {
                        player.dropMessage("Unable to restart event due to an error");
                    }
                }
                player.dropMessage("Done!");
            }
        } else if (command.equals("wipe")) {
            if (args.length() >= 1) {
                boolean wiped = MapleCharacter.wipe(args.get(0));
                if (wiped) {
                    player.dropMessage(6, "Wipe of the player was successful");
                } else {
                    player.dropMessage(5, "Wipe of player was unsuccessful");
                }

            }
        } else if (command.equals("setgmlevel")) {
            if (args.length() == 2) {
                Integer GMLevel = args.parseNumber(1, int.class);
                if (GMLevel == null) {
                    player.sendMessage(5, args.getFirstError());
                    return;
                }
                MapleCharacter target = ch.getPlayerStorage().getCharacterByName(args.get(0));
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
        } else if (command.equals("setcouple")) {
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
                MapleCharacter target1 = ch.getPlayerStorage().getCharacterByName(args.get(1));
                MapleCharacter target2 = ch.getPlayerStorage().getCharacterByName(args.get(2));
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

                final int ringItemID = RingActionHandler.getWeddingRingForEngagementBox(engagementBoxID);
                final int ringID = MapleRing.createRing(ringItemID, target1, target2);
                Equip equip = new Equip(ringItemID, (short) 0);
                equip.setRingId(ringID);
                MapleInventoryManipulator.addFromDrop(target1.getClient(), equip, true);
                rltn.setStatus(Relationship.Status.Married);
                rltn.setEngagementBoxId(engagementBoxID);
                rltn.setBrideId(target2.getId());
                rltn.setGroomId(target1.getId());
                target1.setMarriageRing(MapleRing.loadFromDb(equip.getRingId()));

                equip = new Equip(ringItemID, (short) 0);
                equip.setRingId(ringID + 1);
                MapleInventoryManipulator.addFromDrop(target2.getClient(), equip, true);
                prltn.setStatus(Relationship.Status.Married);
                prltn.setEngagementBoxId(engagementBoxID);
                prltn.setBrideId(target2.getId());
                prltn.setGroomId(target1.getId());
                target2.setMarriageRing(MapleRing.loadFromDb(equip.getRingId()));

                target1.sendMessage(6, "You are now married to '{}'", target2.getName());
                target2.sendMessage(6, "You are now married to '{}'", target1.getName());
                player.sendMessage(6, "Success!");
            } else {
                player.sendMessage(5, "usage: !setcouple <engagement_box> <groom> <bride>");
            }
        }
    }
}
