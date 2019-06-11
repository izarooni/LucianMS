package com.lucianms.command;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SpamTracker;
import com.lucianms.command.executors.*;
import com.lucianms.helpers.JailManager;
import com.lucianms.scheduler.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class CommandWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandWorker.class);
    public static final EventCommands EVENT_COMMANDS = new EventCommands();
    public static final GameMasterCommands GM_COMMANDS = new GameMasterCommands();
    public static final HGMCommands HGM_COMMANDS = new HGMCommands();
    public static final AdministratorCommands ADMIN_COMMANDS = new AdministratorCommands();

    private static final PlayerCommands PLAYER_COMMANDs = new PlayerCommands();

    /**
     * Coming up with new ways to handle commands...
     */
    private CommandWorker() {
    }

    public static boolean isCommand(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        char h = message.charAt(0);
        return h == '!' || h == '@';
    }

    public static boolean process(MapleClient client, String message, boolean noCheck) {
        MapleCharacter player = client.getPlayer();

        char h = message.charAt(0);
        message = message.substring(1);
        int cn = message.indexOf(" "); // command name split index
        String name; // command name
        String[] sp = new String[0]; // args of command
        if (cn > -1) { // a space exists in the message (this assumes there are arguments)
            // there are command arguments
            name = message.substring(0, cn); // substring command name
            if (message.length() > name.length()) { // separate command name from args
                sp = message.substring(cn + 1).split(" ");
            }
        } else {
            // no command arguments
            name = message;
        }

        Command command = new Command(name);
        CommandArgs args = new CommandArgs(sp);

        if (h == '!' && (player.isGM() || noCheck)) {
            TaskExecutor.execute(tryExecuteCommand(client, command, args));
            return true;
        } else if (h == '@') {
            if (!player.isGM() && !noCheck) {
                if (player.getArcade() != null) {
                    player.getArcade().fail();
                }
                if (JailManager.isJailed(player.getId())) {
                    player.sendMessage(5, "You cannot use commands here.");
                    return true;
                } else if (!command.equals("dispose", "quests")) {
                    if ((player.getMapId() >= 90000000 && player.getMapId() <= 90000011) // starter area
                            || player.getMapId() == 80 || player.getMapId() == 81) { // jail
                        player.dropMessage("Commands are disabled in this area.");
                        return true;
                    }
                }
            }
            if (!OccupationCommands.execute(client, command, args)) {
                SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.PlayerCommands);
                if (spamTracker.testFor(1300) && spamTracker.getTriggers() > 3) {
                    player.sendMessage(5, "You are doing this too fast");
                    return true;
                }
                spamTracker.record();

                TaskExecutor.execute(() -> PLAYER_COMMANDs.executeCommand(client, command, args));
            }
            return true;
        }
        return false;
    }

    private static Runnable tryExecuteCommand(MapleClient client, Command cmd, CommandArgs args) {
        return new Runnable() {
            @Override
            public void run() {
                MapleCharacter player = client.getPlayer();
                try {
                    int gmLevel = player.getGMLevel();
                    String name = cmd.getName().toLowerCase();
                    if (EVENT_COMMANDS.getCommands().containsKey(name)) {
                        EVENT_COMMANDS.executeCommand(client, cmd, args);
                    } else if (gmLevel >= 2 && GM_COMMANDS.getCommands().containsKey(name)) {
                        GM_COMMANDS.executeCommand(client, cmd, args);
                    } else if (gmLevel >= 3 && HGM_COMMANDS.getCommands().containsKey(name)) {
                        HGM_COMMANDS.executeCommand(client, cmd, args);
                    } else if (gmLevel >= 4 & ADMIN_COMMANDS.getCommands().containsKey(name)) {
                        ADMIN_COMMANDS.executeCommand(client, cmd, args);
                    } else {
                        player.dropMessage(5, "Command does not exist or you do not have permission to use it");
                    }
                } catch (Exception e) {
                    player.sendMessage(6, "An error curred within this command");
                    LOGGER.error("{} failed to execute command {}", player.toString(), cmd.getName());
                }
            }
        };
    }
}
