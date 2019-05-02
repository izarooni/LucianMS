package com.lucianms.command.executors;

import com.lucianms.client.MapleClient;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.CommandEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class CommandExecutor {

    private HashMap<String, CommandEvent> commands = new HashMap<>(50);

    public Collection<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    public final int getCommandCount() {
        return commands.size();
    }

    public final void addCommand(String commandName, CommandEvent event) {
        commands.put(commandName, event);
    }

    public final Optional<CommandEvent> getCommand(String commandName) {
        return Optional.ofNullable(commands.get(commandName));
    }

    public final boolean executeCommand(MapleClient c, Command cmd, CommandArgs args) {
        String commandName = cmd.getName().toLowerCase();

        Optional<CommandEvent> command = getCommand(commandName);
        if (command.isPresent()) {
            command.get().execute(c.getPlayer(), cmd, args);
            return true;
        } else if (commandName.endsWith("m")) {
            // commands that use the 'm' suffix for map wide effects
            // e.g. warpm, stunm, mutem, etc.
            command = getCommand(commandName.substring(0, commandName.length() - 1));
            command.ifPresent(commandEvent -> commandEvent.execute(c.getPlayer(), cmd, args));
        }
        return false;
    }
}
