package discord.commands;

import discord.Discord;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import tools.Pair;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Manages external command managers for a module command system
 *
 * @author izarooni
 */
public class CommandManagerHelper {

    private static HashMap<String, CommandHelper> managers = new HashMap<>();

    public static void unloadAll() {
        for (Map.Entry<String, CommandHelper> entry : managers.entrySet()) {
            entry.getValue().dispose();
        }
        managers.clear();
    }

    public static Collection<CommandHelper> getManagers() {
        return Collections.unmodifiableCollection(managers.values());
    }

    public static CommandHelper getCommandManager(String name) {
        return managers.get(name);
    }

    public static void addCommandManager(String name, CommandHelper manager) {
        managers.putIfAbsent(name, manager);
    }

    public static boolean isValidCommand(MessageReceivedEvent event) {
        return !event.getMessage().getAuthor().getID().equals(Discord.getBot().getClient().getOurUser().getID()) // bot mustn't self-execute
                && event.getMessage().getContent().startsWith(Discord.getConfig().getString("commandTrigger"));
    }
}
