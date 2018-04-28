package command;

import client.MapleCharacter;
import client.MapleClient;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import scripting.Achievements;
import scripting.event.EventInstanceManager;
import scripting.event.EventManager;
import scripting.map.MapScriptManager;
import server.events.custom.auto.GAutoEvent;
import server.events.custom.auto.GAutoEventManager;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;
import server.maps.PlayerNPC;
import server.quest.custom.CQuestBuilder;

import javax.script.ScriptException;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Level 6 permission requirement
 *
 * @author izarooni
 */
public class AdministratorCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        MapleCharacter player = client.getPlayer();
        World world = client.getWorldServer();

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
                Collections.sort(commands);
                commands.forEach(player::dropMessage);
            } finally {
                commands.clear();
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
                Long a1 = args.parseNumber(0);
                if (args.getError(0) != null) {
                    player.dropMessage(args.getError(0));
                    return;
                }
                int index = a1.intValue() - 1;
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
                    player.dropMessage("Succses!");
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
                        player.sendMessage("{} / id:{} / oid:{} / name:{}", monsters.getPosition().toString(), monsters.getId(), monsters.getObjectId(), monsters.getName());
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
                Long a1 = args.parseNumber(0);
                Long a2 = args.parseNumber(1);
                String error = args.getError(1, 2);
                if (error != null) {
                    player.dropMessage(error);
                    return;
                }
                int x = a1.intValue();
                int y = a2.intValue();
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
        } else if (command.equals("debug")) {
            player.setDebug(!player.isDebug());
            player.sendMessage("Your debug mode is now {}", (player.isDebug() ? "enabled" : "disabled"));
        }
    }
}
