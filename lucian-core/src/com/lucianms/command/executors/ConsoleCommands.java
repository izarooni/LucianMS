package com.lucianms.command.executors;

import com.lucianms.Whitelist;
import com.lucianms.client.MapleCharacter;
import com.lucianms.command.CommandWorker;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.helpers.HouseManager;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.CashShop;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * @author izarooni
 */
public class ConsoleCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommands.class.getSimpleName());
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
                while (reading && (line = scanner.nextLine()) != null) {
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
        if (scanner != null) {
            scanner.close();
        }
    }

    private static void execute(CommandWorker.Command command, CommandWorker.CommandArgs args) {
        if (command.equals("stop", "exit")) {
            if (DiscordSession.getSession() != null) {
                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                mplew.write(Headers.Shutdown.value);
                DiscordSession.sendPacket(mplew.getPacket());
            }

            reading = false;
            System.exit(0);
        } else if (command.equals("reloadmap")) {
            if (args.length() == 1) {
                Integer mapID = args.parseNumber(0, int.class);
                String error = args.getFirstError();
                if (error != null) {
                    LOGGER.warn(error);
                    return;
                }
                for (MapleWorld world : Server.getWorlds()) {
                    for (MapleChannel channel : world.getChannels()) {
                        channel.reloadMap(mapID);
                        LOGGER.info("Reloading map {} in world {} channel {}", mapID, (world.getId() + 1), channel.getId());
                    }
                }
            }
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
                        try {
                            Server.reloadConfig();
                            LOGGER.info("Server configuration reloaded!");
                        } catch (FileNotFoundException e) {
                            LOGGER.error("Failed to reload config", e);
                        }
                        break;
                    }
                }
            } else {
                LOGGER.info("Available operations: cs, whitelist, cquests, achievements, houses, config");
            }
        } else if (command.equals("online")) {
            for (MapleWorld worlds : Server.getWorlds()) {
                LOGGER.info("World {}:", (worlds.getId() + 1));
                for (MapleChannel channels : worlds.getChannels()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\tChannel {}: ");
                    for (MapleCharacter players : channels.getPlayerStorage().values()) {
                        sb.append(players.getName()).append(", ");
                    }
                    if (sb.length() > 2) {
                        sb.setLength(sb.length() - 2);
                    }
                    LOGGER.info(sb.toString(), channels.getId());
                    LOGGER.info("");
                }
            }
        } else if (command.equals("gc")) {
            TaskExecutor.purge();
            LOGGER.info("Tasks purged");
            System.gc();
            LOGGER.info("GC requested");
        } else if (command.equals("crash")) {
            if (args.length() == 1) {
                String username = args.get(0);
                for (MapleWorld world : Server.getWorlds()) {
                    MapleCharacter player = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (player != null) {
                        player.getClient().disconnect(false, player.getCashShop().isOpened());
                        world.removePlayer(player);
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
            LOGGER.info("reloadmap <map_id> - Reload an in-game map");
            LOGGER.info("reload <operation> - Reload/clear the cache of specified feature");
        }
    }
}
