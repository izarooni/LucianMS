package com.lucianms.command;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.scripting.Achievements;
import server.CashShop;
import server.Whitelist;
import server.quest.custom.CQuestBuilder;
import server.world.HouseManager;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * @author izarooni
 */
public class ConsoleCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommands.class);
    private static volatile boolean reading = false;
    private static Scanner scanner;

    private ConsoleCommands() {
    }

    public static void beginReading() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reading = true;
                scanner = new Scanner(System.in);
                String line;
                while (scanner.hasNext() && reading && (line = scanner.nextLine()) != null) {
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

                    try {
                        execute(command, args);
                    } catch (Throwable t) {
                        // don't break the loop
                        t.printStackTrace();
                    }
                }
                LOGGER.info("Console no longer reading commands");
            }
        }, "ConsoleReader").start();
    }

    public static void stopReading() {
        reading = false;
        scanner.close();
    }

    private static void execute(CommandWorker.Command command, CommandWorker.CommandArgs args) {
        if (command.equals("shutdown", "exit")) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.write(Headers.Shutdown.value);
            DiscordSession.sendPacket(mplew.getPacket());

            reading = false;
            System.exit(0);
        } else if (command.equals("reload")) {
            if (args.length() == 1) {
                switch (args.get(0)) {
                    default:
                        LOGGER.info("Available operations: cs, whitelist, cquests, achievements, houses, config");
                        break;
                    case "cs":
                        CashShop.CashItemFactory.loadCommodities();
                        LOGGER.info("Cash Shop commodities reloaded");
                        break;
                    case "whitelist":
                        try {
                            final int bCount = Whitelist.getAccounts().size();
                            Whitelist.loadAccounts();
                            final int aCount = Whitelist.getAccounts().size();
                            LOGGER.info("Whitelist reloaded. Previously had {} accounts, now {}", bCount, aCount);
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "cquests":
                        CQuestBuilder.loadAllQuests();
                        break;
                    case "achievements": {
                        int count = Achievements.loadAchievements();
                        LOGGER.info("Reloaded {} achievements", count);
                        break;
                    }
                    case "houses": {
                        int count = HouseManager.loadHouses();
                        LOGGER.info("Reloaded {} houses", count);
                        break;
                    }
                    case "config": {
                        Server.getInstance().getConfig().clearCaches();
                        LOGGER.info("Server configuration cache cleared!");
                        break;
                    }
                }
            } else {
                LOGGER.info("Available operations: cs, whitelist, cquests, achievements, houses, config");
            }
        } else if (command.equals("online")) {
            LOGGER.info("Managed LOGIN Sessions: " + Server.getInstance().getAcceptor().getManagedSessionCount());
            if (Server.getInstance().isOnline()) {
                for (World worlds : Server.getInstance().getWorlds()) {
                    LOGGER.info("World {}:", (worlds.getId() + 1));
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
        } else if (command.equals("gc")) {
            TaskExecutor.purge();
            LOGGER.info("Tasks purged");
            System.gc();
            LOGGER.info("GC requested");
        } else if (command.equals("crash")) {
            if (args.length() == 1) {
                String username = args.get(0);
                for (World world : Server.getInstance().getWorlds()) {
                    MapleCharacter player = world.getPlayerStorage().getCharacterByName(username);
                    if (player != null) {
                        player.getClient().disconnect(false, player.getCashShop().isOpened());
                        LOGGER.info("{} disconnected", player.getName());
                        return;
                    }
                }
                LOGGER.info("Unable to find any player named {}", username);
            }
        } else if (command.equals("cls")) {
            if (args.length() == 1) {
                Integer amount = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    LOGGER.info(error);
                    return;
                }
                if (amount < 0) {
                    LOGGER.info("You must enter a number larger than 0");
                    return;
                }
                for (int i = 0; i < amount; i++) {
                    System.out.println();
                }
            }
        } else if (command.equals("help", "commands")) {
            LOGGER.info("gc - Requests JVM garbage collection");
            LOGGER.info("cls <count> - \"Clear\" the buffer");
            LOGGER.info("exit - Safely stop and close the server");
            LOGGER.info("crash <username> - Crash an in-game character");
            LOGGER.info("online - View current online players");
            LOGGER.info("reload <operation> - Reload/clear the cache of specified feature");
        }
    }
}
