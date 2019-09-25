package com.lucianms.command.executors;

import com.lucianms.Whitelist;
import com.lucianms.client.MapleCharacter;
import com.lucianms.command.Command;
import com.lucianms.command.CommandArgs;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.helpers.HouseManager;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.CashShop;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author izarooni
 */
public class ChannelConsoleCommands extends ConsoleCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelConsoleCommands.class);

    @Override
    public void execute(Command command, CommandArgs args) {
        if (command.equals("stop", "exit")) {
            if (DiscordConnection.getSession() != null) {
                MaplePacketWriter w = new MaplePacketWriter();
                w.write(Headers.Shutdown.value);
                DiscordConnection.sendPacket(w.getPacket());
            }
            stopReading();
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
                            LOGGER.info("Whitelist reloaded. Previously had {} accounts, now {}", bCount, Whitelist.createCache());
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
            for (MapleWorld world : Server.getWorlds()) {
                System.out.printf("World %d:\r\n", (world.getId() + 1));
                for (MapleChannel ch : world.getChannels()) {
                    System.out.printf("\tChannel %d: ", ch.getId());
                    StringBuilder usernames = new StringBuilder();
                    Collection<MapleCharacter> players = world.getPlayers(p -> p.getClient().getChannel() == ch.getId());
                    for (MapleCharacter player : players) {
                        usernames.append(player.getName()).append(", ");
                    }
                    players.clear();
                    if (usernames.length() > 2) {
                        usernames.setLength(usernames.length() - 2);
                    }
                    System.out.printf("%s\r\n", usernames.toString());
                    usernames.setLength(0);
                }
            }
        } else if (command.equals("gc")) {
            TaskExecutor.getExecutor().purge();
            LOGGER.info("Tasks purged");
            System.gc();
            LOGGER.info("GC requested");
        } else if (command.equals("crash")) {
            if (args.length() == 1) {
                String username = args.get(0);
                for (MapleWorld world : Server.getWorlds()) {
                    MapleCharacter player = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
                    if (player != null) {
                        world.getPlayerStorage().remove(player.getId());
                        player.getClient().disconnect();
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
            ArrayList<String> desc = new ArrayList<>();
            desc.add("gc - Requests JVM garbage collection");
            desc.add("cls <count> - \"Clear\" the buffer");
            desc.add("exit - Safely stop and close the server");
            desc.add("crash <username> - Crash an in-game character");
            desc.add("online - View current online players");
            desc.add("reloadmap <map_id> - Reload an in-game map");
            desc.add("reload <operation> - Reload/clear the cache of specified feature");
            desc.forEach(System.out::println);
            desc.clear();
        }
    }
}
