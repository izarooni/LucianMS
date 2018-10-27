package com.lucianms.server;

import com.lucianms.Whitelist;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SkillFactory;
import com.lucianms.command.executors.ConsoleCommands;
import com.lucianms.constants.ServerConstants;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.features.auto.GAutoEventManager;
import com.lucianms.features.scheduled.SOuterSpace;
import com.lucianms.helpers.DailyWorker;
import com.lucianms.helpers.HouseManager;
import com.lucianms.helpers.RankingWorker;
import com.lucianms.io.Config;
import com.lucianms.io.defaults.Defaults;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.lang.GProperties;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.CashShop.CashItemFactory;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleAlliance;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.MapleWorld;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import tools.Database;
import tools.Pair;
import tools.StringUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Server {

    public enum RunningOperation {
        Login, Channel
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    public static final long Uptime = System.currentTimeMillis();

    private static Task dailyTask = null;
    private static final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
    private static final ArrayList<MapleWorld> worlds = new ArrayList<>();
    private static final ArrayList<Map<Integer, String>> channels = new ArrayList<>();
    private static final ArrayList<Pair<Integer, String>> worldRecommendedList = new ArrayList<>();
    private static final ConcurrentHashMap<Integer, MapleGuild> guilds = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, MapleAlliance> alliances = new ConcurrentHashMap<>();
    private static final GProperties<Boolean> toggles = new GProperties<>();

    private static HikariDataSource hikari = Database.createDataSource("hikari-server");
    private static Config config = null;

    public static Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public static GProperties<Boolean> getToggles() {
        return toggles;
    }

    @Deprecated
    public static void insertLog(String author, String description, Object... args) {
        try (Connection con = hikari.getConnection(); PreparedStatement ps = con.prepareStatement("insert into loggers (author, description) values (?, ?)")) {
            ps.setString(1, author);
            ps.setString(2, MessageFormatter.arrayFormat(description, args).getMessage());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.info("Unable to insert log from '{}': {}", author, description, e);
        }
    }

    public static ArrayList<Pair<Integer, String>> worldRecommendedList() {
        return worldRecommendedList;
    }

    public static void removeChannel(int worldid, int channel) {
        channels.remove(channel);

        MapleWorld world = worlds.get(worldid);
        if (world != null) {
            world.removeChannel(channel);
        }
    }

    public static MapleChannel getChannel(int world, int channel) {
        return worlds.get(world).getChannel(channel);
    }

    public static List<MapleChannel> getChannelsFromWorld(int world) {
        return worlds.get(world).getChannels();
    }

    public static List<MapleChannel> getAllChannels() {
        List<MapleChannel> channelz = new ArrayList<>();
        for (MapleWorld world : worlds) {
            channelz.addAll(world.getChannels());
        }
        return channelz;
    }

    public static String getIP(int world, int channel) {
        return channels.get(world).get(channel);
    }

    public static void createServer(RunningOperation operation) {
        final long beginning = System.currentTimeMillis();
        long timeToTake;

        Properties p = new Properties();
        try {
            p.load(new FileInputStream("world.ini"));
        } catch (Exception e) {
            LOGGER.info("Please start create_server.bat");
            System.exit(0);
            return;
        }

        try {
            if (Defaults.createDefaultIfAbsent(null, "server-config.json")) {
                LOGGER.info("Server config created. Configure settings and restart the server");
                System.exit(0);
                return;
            }
            reloadConfig();
            if (getConfig().getBoolean("WhitelistEnabled")) {
                if (Defaults.createDefaultIfAbsent(null, "whitelist.json")) {
                    LOGGER.info("Whitelist file created");
                }
                Whitelist.loadAccounts();
                LOGGER.info(Whitelist.getAccounts().size() + " whitelisted accounts loaded");
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }


        TaskExecutor.prestartAllCoreThreads();
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown()));

        TaskExecutor.createRepeatingTask(RankingWorker::new, ServerConstants.RANKING_INTERVAL);

        // schedule a daily task
        Calendar tmrw = Calendar.getInstance();
        tmrw.set(Calendar.DATE, tmrw.get(Calendar.DATE) + 1);
        tmrw.set(Calendar.HOUR_OF_DAY, 2);
        tmrw.set(Calendar.MINUTE, 0);
        tmrw.set(Calendar.SECOND, 0);
        tmrw.set(Calendar.MILLISECOND, 0);
        dailyTask = TaskExecutor.createRepeatingTask(new DailyWorker(), (tmrw.getTimeInMillis() - System.currentTimeMillis()), TimeUnit.DAYS.toMillis(1));
        LOGGER.info("Entry reset scheduled to run in {}", StringUtil.getTimeElapse(tmrw.getTimeInMillis() - System.currentTimeMillis()));

        if (operation == RunningOperation.Channel) {
            timeToTake = System.currentTimeMillis();
            MapleQuest.loadAllQuest();
            CQuestBuilder.loadAllQuests();
            LOGGER.info("Quest data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

            timeToTake = System.currentTimeMillis();
            SkillFactory.loadAllSkills();
            LOGGER.info("Skill data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

            Achievements.loadAchievements();
        }
        timeToTake = System.currentTimeMillis();
        MapleItemInformationProvider.getInstance().getAllItems();
        CashItemFactory.loadCommodities();
        LOGGER.info("Item data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        try {
            timeToTake = System.currentTimeMillis();
            int qWorlds = Integer.parseInt(p.getProperty("worlds"));
            for (int i = 0; i < qWorlds; i++) {
                int flag = Integer.parseInt(p.getProperty("flag" + i));
                String eMessage = p.getProperty("eventmessage" + i);
                String sMessage = p.getProperty("servermessage" + i);

                MapleWorld world = new MapleWorld(i, flag, eMessage, Integer.parseInt(p.getProperty("exprate" + i)), Integer.parseInt(p.getProperty("droprate" + i)), Integer.parseInt(p.getProperty("mesorate" + i)), Integer.parseInt(p.getProperty("bossdroprate" + i)));

                worlds.add(world);
                worldRecommendedList.add(new Pair<>(i, p.getProperty("recommend" + i)));

                channels.add(new LinkedHashMap<>());
                int qChannels = Integer.parseInt(p.getProperty("channels" + i));
                for (int j = 0; j < qChannels; j++) {
                    int channelId = j + 1;
                    MapleChannel channel = new MapleChannel(i, channelId);
                    world.addChannel(channel);
                    channels.get(i).put(channelId, channel.getIP());
                }
                world.setServerMessage(sMessage);

                final long repeat = (1000 * 60 * 60) * 4;
                TaskExecutor.createRepeatingTask(() -> GAutoEventManager.startRandomEvent(world), repeat);
                world.addScheduledEvent(new SOuterSpace(world));
                LOGGER.info("World {} created {} channels in {}s", (world.getId() + 1), world.getChannels().size(), ((System.currentTimeMillis() - timeToTake) / 1000d));
            }

            if (operation == RunningOperation.Channel) {
                timeToTake = System.currentTimeMillis();
                int count = HouseManager.loadHouses();
                LOGGER.info("{} houses loaded in {}s", count, ((System.currentTimeMillis() - timeToTake) / 1000d));
            }
        } catch (Exception e) {
            e.printStackTrace();// For those who get errors
            System.exit(0);
            return;
        }

//        ConsoleCommands.beginReading();
    }

    public static void reloadConfig() throws FileNotFoundException {
        config = new Config(new JSONObject(new JSONTokener(new FileInputStream("server-config.json"))));
    }

    public static Config getConfig() {
        return config;
    }

    public static MapleAlliance getAlliance(int id) {
        return alliances.computeIfAbsent(id, i -> null);
    }

    public static void addAlliance(int id, MapleAlliance alliance) {
        alliances.put(id, alliance);
    }

    public static void disbandAlliance(int id) {
        MapleAlliance alliance = alliances.get(id);
        if (alliance != null) {
            for (Integer gid : alliance.getGuilds()) {
                guilds.get(gid).setAllianceId(0);
            }
            alliances.remove(id);
        }
    }

    public static void allianceMessage(int id, final byte[] packet, int exception, int guildex) {
        MapleAlliance alliance = alliances.get(id);
        if (alliance != null) {
            for (Integer gid : alliance.getGuilds()) {
                if (guildex == gid) {
                    continue;
                }
                MapleGuild guild = guilds.get(gid);
                if (guild != null) {
                    guild.broadcast(packet, exception);
                }
            }
        }
    }

    public static boolean addGuildtoAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.addGuild(guildId);
            return true;
        }
        return false;
    }

    public static boolean removeGuildFromAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.removeGuild(guildId);
            return true;
        }
        return false;
    }

    public static boolean setAllianceRanks(int aId, String[] ranks) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setRankTitle(ranks);
            return true;
        }
        return false;
    }

    public static boolean setAllianceNotice(int aId, String notice) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setNotice(notice);
            return true;
        }
        return false;
    }

    public static boolean increaseAllianceCapacity(int aId, int inc) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.increaseCapacity(inc);
            return true;
        }
        return false;
    }

    public static Set<Integer> getChannelServer(int world) {
        return new HashSet<>(channels.get(world).keySet());
    }

    public static byte getHighestChannelId() {
        byte highest = 0;
        for (Iterator<Integer> it = channels.get(0).keySet().iterator(); it.hasNext(); ) {
            Integer channel = it.next();
            if (channel != null && channel > highest) {
                highest = channel.byteValue();
            }
        }
        return highest;
    }

    public static int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }

    public static MapleGuild getGuild(int id, int world, MapleGuildCharacter mgc) {
        synchronized (guilds) {
            if (guilds.get(id) != null) {
                return guilds.get(id);
            }
            MapleGuild g = new MapleGuild(id, world);
            if (g.getId() == -1) {
                return null;
            }
            if (mgc != null) {
                g.setOnline(mgc.getId(), true, mgc.getChannel());
            }
            guilds.put(id, g);
            return g;
        }
    }

    public static void clearGuilds() {// remake
        synchronized (guilds) {
            guilds.clear();
        }
        // for (List<Channel> world : worlds.values()) {
        // reloadGuildCharacters();
    }

    public static void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) {
        MapleGuild g = getGuild(mgc.getGuildId(), mgc.getWorld(), mgc);
        g.setOnline(mgc.getId(), bOnline, channel);
    }

    public static int addGuildMember(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mgc);
        }
        return 0;
    }

    public static boolean setGuildAllianceId(int gId, int aId) {
        MapleGuild guild = guilds.get(gId);
        if (guild != null) {
            guild.setAllianceId(aId);
            return true;
        }
        return false;
    }

    public static void leaveGuild(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public static void guildChat(int gid, String name, int cid, String msg) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public static void changeRank(int gid, int cid, int newRank) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRank(cid, newRank);
        }
    }

    public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        MapleGuild g = guilds.get(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public static void setGuildNotice(int gid, String notice) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public static void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mgc);
        }
    }

    public static void changeRankTitle(int gid, String[] ranks) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public static void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public static void disbandGuild(int gid) {
        synchronized (guilds) {
            MapleGuild g = guilds.get(gid);
            g.disbandGuild();
            guilds.remove(gid);
        }
    }

    public static boolean increaseGuildCapacity(int gid) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public static void gainGP(int gid, int amount) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public static void guildMessage(int gid, byte[] packet) {
        guildMessage(gid, packet, -1);
    }

    public static void guildMessage(int gid, byte[] packet, int exception) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.broadcast(packet, exception);
        }
    }

    public static PlayerBuffStorage getPlayerBuffStorage() {
        return buffStorage;
    }

    public static void deleteGuildCharacter(MapleGuildCharacter mgc) {
        setGuildMemberOnline(mgc, false, (byte) -1);
        if (mgc.getGuildRank() > 1) {
            leaveGuild(mgc);
        } else {
            disbandGuild(mgc.getGuildId());
        }
    }

    public static void reloadGuildCharacters(int world) {
        MapleWorld worlda = getWorld(world);
        for (MapleChannel channel : worlda.getChannels()) {
            for (MapleCharacter mc : channel.getPlayerStorage().values()) {
                if (mc.getGuildId() > 0) {
                    setGuildMemberOnline(mc.getMGC(), true, worlda.getId());
                    memberLevelJobUpdate(mc.getMGC());
                }
            }
            worlda.reloadGuildSummary();
        }
    }

    public static void broadcastMessage(final byte[] packet) {
        for (MapleChannel ch : getChannelsFromWorld(0)) {
            ch.broadcastPacket(packet);
        }
    }

    public static void broadcastGMMessage(final byte[] packet) {
        for (MapleChannel ch : getChannelsFromWorld(0)) {
            ch.broadcastGMPacket(packet);
        }
    }

    public static MapleWorld getWorld(int id) {
        return worlds.get(id);
    }

    public static List<MapleWorld> getWorlds() {
        return worlds;
    }

    private static Runnable shutdown() {// only once :D
        return new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Shutdown hook invoked");

                LOGGER.info("Login server closed");

                getWorlds().forEach(MapleWorld::shutdown);

                TaskExecutor.cancelTask(dailyTask);
                TaskExecutor.shutdownNow();

                LOGGER.info("Worlds & channels are now offline");

                ConsoleCommands.stopReading();

//                DiscordSession.getDiscordServer().close(); // todo shutdown
            }
        };
    }
}