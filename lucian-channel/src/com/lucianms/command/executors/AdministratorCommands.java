package com.lucianms.command.executors;

import com.lucianms.client.*;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.meta.Occupation;
import com.lucianms.command.CommandWorker;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.events.PlayerRingActionEvent;
import com.lucianms.features.auto.GAutoEvent;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.features.scheduled.SAutoEvent;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventManager;
import com.lucianms.io.scripting.map.FieldScriptExecutor;
import com.lucianms.io.scripting.portal.PortalScriptManager;
import com.lucianms.io.scripting.reactor.ReactorScriptManager;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleShopFactory;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterInformationProvider;
import com.lucianms.server.life.MapleNPC;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleReactor;
import com.lucianms.server.maps.PlayerNPC;
import com.lucianms.server.world.MapleWorld;

import javax.script.ScriptException;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * Level 6 permission requirement
 *
 * @author izarooni
 */
public class AdministratorCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        MapleCharacter player = client.getPlayer();
        MapleWorld world = client.getWorldServer();
        MapleChannel ch = client.getChannelServer();

        if (command.equals("admincommands")) {
            ArrayList<String> commands = new ArrayList<>();
            try {
                commands.add("!clearcache - Clear the cache of a specified system");
                commands.add("!fae - Force an auto event execution");
                commands.add("!list - Debug command");
                commands.add("!reloadevent - Reload a specified event script");
                commands.add("!wpos - Warp yourself to a specified {x,y} position in the current map");
                commands.add("!setgmlevel - Change the GM level of a specified player");
                commands.add("!setcouple - Declare two specified players as a married couple");
                commands.add("!occupation - Changes your occupation");
                commands.sort(String::compareTo);
                commands.forEach(player::dropMessage);
            } finally {
                commands.clear();
            }
        } else if (command.equals("sethp")) {
            if (args.length() == 1) {
                Integer hp = args.parseNumber(0, int.class);
                if (hp != null) {
                    player.setHp(hp);
                    player.updateSingleStat(MapleStat.HP, hp);
                }
            }
        } else if (command.equals("clearcache")) {
            if (args.length() == 1) {
                switch (args.get(0)) {
                    default:
                        return;
                    case "achievements":
                        Achievements.loadAchievements();
                        player.sendMessage("Achievement scripts reloaded");
                        break;
                    case "skills":
                        SkillFactory.loadAllSkills();
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
        } else if (command.equals("setrates")) {
            if (args.length() == 3) {
                Integer expRate = args.parseNumber(0, int.class);
                Integer mesoRate = args.parseNumber(1, int.class);
                Integer dropRate = args.parseNumber(2, int.class);
                if (expRate == null || mesoRate == null || dropRate == null) {
                    player.sendMessage(5, args.getFirstError());
                    return;
                }
                world.setExpRate(expRate);
                world.setMesoRate(mesoRate);
                world.setDropRate(dropRate);
                world.updateRates();
                world.broadcastMessage(6, "The Exp, Meso and Drop rate have changed to {}x {}x and {}x respectively", expRate, mesoRate, dropRate);
            }
        } else if (command.equals("sae")) {
            if (args.length() == 1) {
                SAutoEvent scheduledEvent = world.getScheduledEvent(args.get(0));
                scheduledEvent.run();
            } else {
                player.sendMessage(5, "Usage: !autoevent <event_name>");
            }
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
                    GAutoEvent gEvent = event.getClazz().getDeclaredConstructor(MapleWorld.class).newInstance(client.getWorldServer());
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
            for (MapleWorld worlds : Server.getWorlds()) {
                for (MapleChannel channels : worlds.getChannels()) {
                    channels.reloadEventScriptManager();
                }
            }
            player.dropMessage(6, "Event script reloaded");
        } else if (command.equals("reloadevent")) {
            if (args.length() == 1) {
                String scriptName = args.get(0);
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
                MapleCharacter target = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(0)));
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
                MapleCharacter target1 = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(1)));
                MapleCharacter target2 = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(args.get(2)));
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
        } else if (command.equals("occupation")) {
            if (args.length() != 1) {
                player.dropMessage("usage: !occupation <occupation_name>");
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
}
