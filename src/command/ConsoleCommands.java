package command;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

import client.MapleCharacter;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import server.Whitelist;

/**
 * @author izarooni
 */
public class ConsoleCommands {

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
                System.out.println("Console no longer reading commands");
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
                    System.out.println("World " + worlds.getId() + ": ");
                    for (Channel channels : worlds.getChannels()) {
                        System.out.println("\tChannel " + channels.getId() + ": ");
                        StringBuilder sb = new StringBuilder();
                        for (MapleCharacter players : channels.getPlayerStorage().getAllCharacters()) {
                            sb.append(players.getName()).append(", ");
                        }
                        if (sb.length() > 2) {
                            sb.setLength(sb.length() - 2);
                        }
                        System.out.println("\t\t" + sb.toString());
                        System.out.println("");
                    }
                }
            } else {
                System.err.println("The server is not online!");
            }
        } else if (command.equals("reloadwhitelist")) {
            try {
                final int bCount = Whitelist.getAccounts().size();
                Whitelist.loadAccounts();
                final int aCount = Whitelist.getAccounts().size();
                System.out.println(String.format("Whitelist reloaded from %d accounts to %d", bCount, aCount));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}