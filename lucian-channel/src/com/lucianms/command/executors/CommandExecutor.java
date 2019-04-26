package com.lucianms.command.executors;

import com.lucianms.client.MapleClient;
import com.lucianms.command.CommandEvent;
import com.lucianms.command.CommandWorker;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class CommandExecutor {

    private HashMap<String, CommandEvent> commands = new HashMap<>(50);

    public Collection<String> getCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    public final void addCommand(String commandName, CommandEvent event) {
        commands.put(commandName, event);
    }

    public final Optional<CommandEvent> getCommand(String commandName) {
        return Optional.ofNullable(commands.get(commandName));
    }

    public final boolean executeCommand(MapleClient c, CommandWorker.Command cmd, CommandWorker.CommandArgs args) {
        Optional<CommandEvent> command = getCommand(cmd.getName());
        if (command.isPresent()) {
            command.get().execute(c.getPlayer(), cmd, args);
            return true;
        }
        return false;
    }
}
