package command;

import client.MapleCharacter;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Whitelist;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * @author izarooni
 */
public class ConsoleCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommands.class);
    private static volatile boolean reading = false;

    private ConsoleCommands() {
    }

    public static void beginReading() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                reading = true;
                while (reading) {
                    String line = scanner.nextLine();
                    if (line != null && !line.isEmpty()) {
                        // from CommandWorker
                        int cn = line.indexOf(" "); // command name split index
                        String name; // command name
                        String[] sp = new String[0]; // args of command
                        if (cn > -1) { // a space exists in the message (this assumes there are arguments)
                            // there are command arguments
                            name = line.substring(0, cn); // substring command name
                            if (line.length() > name.length()) { // separate command name from args
                                sp = line.substring(cn + 1, line.length()).split(" ");
                            }
                        } else {
                            // no command arguments
                            name = line;
                        }

                        CommandWorker.Command command = new CommandWorker.Command(name);
                        CommandWorker.CommandArgs args = new CommandWorker.CommandArgs(sp);

                        execute(command, args);
                    }
                }
                scanner.close();
                LOGGER.info("Console no longer reading commands");
            }
        }, "ConsoleReader").start();
    }

    public static void stopReading() {
        reading = false;
    }

    private static void execute(CommandWorker.Command command, CommandWorker.CommandArgs args) {
        if (command.equals("shutdown", "exit")) {
            reading = false;
            System.exit(0);
        } else if (command.equals("online")) {
            if (Server.getInstance().isOnline()) {
                for (World worlds : Server.getInstance().getWorlds()) {
                    LOGGER.info("World {}:", worlds.getId());
                    for (Channel channels : worlds.getChannels()) {
                        LOGGER.info("\tChannel {}:", channels.getId());
                        StringBuilder sb = new StringBuilder();
                        for (MapleCharacter players : channels.getPlayerStorage().getAllCharacters()) {
                            sb.append(players.getName()).append(", ");
                        }
                        if (sb.length() > 2) {
                            sb.setLength(sb.length() - 2);
                        }
                        LOGGER.info("\t\t{}", sb.toString());
                        LOGGER.info("");
                    }
                }
            } else {
                LOGGER.error("The server is not online!");
            }
        } else if (command.equals("reloadwhitelist")) {
            try {
                final int bCount = Whitelist.getAccounts().size();
                Whitelist.loadAccounts();
                final int aCount = Whitelist.getAccounts().size();
                LOGGER.info("Whitelist reloaded. Previously had {} accounts, now has {}", bCount, aCount);
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else if (command.equals("gc")) {
            System.gc();
            LOGGER.info("GC requested");
        }
    }
}
