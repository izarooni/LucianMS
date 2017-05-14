package client.command;

import client.MapleCharacter;
import client.MapleClient;
import net.server.world.World;
import server.events.custom.auto.GAutoEvent;
import server.events.custom.auto.GAutoEventManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Level 6 permission requirement
 *
 * @author izarooni
 */
public class AdministratorCommands {

    public static void execute(MapleClient client, CommandWorker.Command command, CommandWorker.CommandArgs args) {

        MapleCharacter player = client.getPlayer();

        if (command.equals("fae")) { // force auto event
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
        }
    }
}
