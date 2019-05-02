package com.lucianms.commands;

import com.lucianms.BanManager;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.command.executors.ConsoleCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class LoginConsoleCommands extends ConsoleCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginConsoleCommands.class);

    @Override
    public void execute(Command command, CommandArgs args) {
        if (command.equals("unban")) {
            if (args.length() != 1) {
                LOGGER.info("Usage: unban <username>");
                return;
            }
            String username = args.get(0);
            if (BanManager.pardonUser(username)) {
                LOGGER.info("Successfully unbanned '{}' and all relating data (machineID, MAC, IP)", username);
            } else {
                LOGGER.info("Failed to find any user account via username '{}", username);
            }
        }
    }
}
