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

    private static HashMap<String, Pair<URLClassLoader, CommandHelper>> managers = new HashMap<>();

    public static void unloadAll() {
        for (Map.Entry<String, Pair<URLClassLoader, CommandHelper>> entry : managers.entrySet()) {
            entry.getValue().getRight().onUnload();
            try {
                entry.getValue().getLeft().close();
            } catch (IOException e) {
                System.err.println(String.format("Unable to unload command worker '%s': %s", entry.getKey(), e.getMessage()));
            }

        }
        managers.clear();
    }

    public static Collection<CommandHelper> getManagers() {
        ArrayList<CommandHelper> ret = new ArrayList<>(managers.size());
        managers.values().forEach(p -> ret.add(p.getRight()));
        return Collections.unmodifiableList(ret);
    }

    public static CommandHelper getCommandManager(String name) {
        if (!managers.containsKey(name)) {
            return null;
        }
        return managers.get(name).getRight();
    }

    public static void addCommandManager(String name, URLClassLoader loader, CommandHelper manager) {
        managers.putIfAbsent(name, new Pair<>(loader, manager));
    }

    public static boolean isValidCommand(MessageReceivedEvent event) {
        return !event.getMessage().getAuthor().getID().equals(Discord.getBot().getClient().getOurUser().getID()) // bot mustn't self-execute
                && event.getMessage().getContent().startsWith(Discord.getConfig().getString("commandTrigger"));
    }
}
