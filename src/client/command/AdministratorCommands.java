package client.command;

import client.MapleCharacter;
import client.MapleClient;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import scripting.event.EventManager;
import server.events.custom.auto.GAutoEvent;
import server.events.custom.auto.GAutoEventManager;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;

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

        if (command.equals("help")) {
            ArrayList<String> commands = new ArrayList<>();
            try {
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
                    for (MapleMapObject object : player.getMap().getAllReactor()) {
                        MapleReactor reactor = (MapleReactor) object;
                        player.dropMessage(String.format("{%s / id:%d / oid:%d / name:%s}", reactor.getPosition().toString(), reactor.getId(), reactor.getObjectId(), reactor.getName()));
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
                        break;
                    }
                    manager.dispose();
                    channel.getEventScriptManager().putManager(scriptName);
                    try {
                        EventManager em = channel.getEventScriptManager().getManager(scriptName);
                        try {
                            em.getInvocable().invokeFunction("init", (Object) null);
                        } catch (ScriptException | NoSuchMethodException e) {
                            player.dropMessage("");
                            e.printStackTrace();
                        }
                    } catch (RuntimeException e) {
                        player.dropMessage("Unable to restart event due to an error");
                    }
                }
                player.dropMessage("Done!");
            }
        } else if(command.equals("wipe")) {
        	if(args.length() >= 1) {
        		boolean wiped = MapleCharacter.wipe(args.get(0));
        		
        		if(wiped) {
        			player.dropMessage(6, "Wipe of the player was successful");
        		} else {
        			player.dropMessage(5, "Wipe of player was unsuccessful");
        		}
        		
        	}
        }
    }
}
