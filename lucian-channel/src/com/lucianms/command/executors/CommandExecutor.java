package com.lucianms.command.executors;

import com.lucianms.client.MapleClient;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;
import tools.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandExecutor {

    private final HashMap<String, Pair<CommandEvent, String>> commands = new HashMap<>(50);

    public Map<String, Pair<CommandEvent, String>> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public final void addCommand(String commandName, CommandEvent event, String description) {
        commands.put(commandName, new Pair<>(event, description));
    }

    public final Optional<Pair<CommandEvent, String>> getCommand(String commandName) {
        return Optional.ofNullable(commands.get(commandName));
    }

    public final boolean executeCommand(MapleClient c, Command cmd, CommandArgs args) {
        String commandName = cmd.getName().toLowerCase();

        Optional<Pair<CommandEvent, String>> command = getCommand(commandName);
        if (command.isPresent()) {
            command.get().getLeft().execute(c.getPlayer(), cmd, args);
            return true;
        } else if (commandName.endsWith("m")) {
            // commands that use the 'm' suffix for map wide effects
            // e.g. warpm, stunm, mutem, etc.
            command = getCommand(commandName.substring(0, commandName.length() - 1));
            command.ifPresent(commandEvent -> commandEvent.getLeft().execute(c.getPlayer(), cmd, args));
        }
        return false;
    }
}
