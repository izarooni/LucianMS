/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server;

import client.MapleCharacter;
import client.SkillFactory;
import com.lucianms.command.ConsoleCommands;
import constants.ServerConstants;
import com.lucianms.io.Config;
import com.lucianms.io.defaults.Defaults;
import net.MapleServerHandler;
import com.lucianms.discord.DiscordSession;
import net.mina.MapleCodecFactory;
import net.server.channel.Channel;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.World;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.io.scripting.Achievements;
import server.CashShop.CashItemFactory;
import server.MapleItemInformationProvider;
import com.lucianms.server.Whitelist;
import com.lucianms.features.scheduled.SOuterSpace;
import server.quest.MapleQuest;
import com.lucianms.cquest.CQuestBuilder;
import com.lucianms.helpers.HouseManager;
import tools.DatabaseConnection;
import tools.Pair;
import tools.StringUtil;
import com.lucianms.helpers.DailyWorker;
import com.lucianms.helpers.RankingWorker;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    public static final long Uptime = System.currentTimeMillis();

    private static Server instance = null;
    private IoAcceptor acceptor;
    private List<Map<Integer, String>> channels = new LinkedList<>();
    private List<World> worlds = new ArrayList<>();
    private List<Pair<Integer, String>> worldRecommendedList = new LinkedList<>();
    private final Map<Integer, MapleGuild> guilds = new LinkedHashMap<>();
    private final Map<Integer, MapleAlliance> alliances = new LinkedHashMap<>();
    private PlayerBuffStorage buffStorage = new PlayerBuffStorage();
    private boolean online = false;
    private Task dailyTask = null;

    private Properties subnetInfo = new Properties();
    private Config config = null;

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public static void insertLog(String author, String description, Object... args) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("insert into loggers (author, description) values (?, ?)")) {
            ps.setString(1, author);
            ps.setString(2, MessageFormatter.arrayFormat(description, args).getMessage());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.info("Unable to insert log from '{}': {}", author, description, e);
        }
    }

    public static void main(String args[]) {
        Server.getInstance().run();
    }

    public IoAcceptor getAcceptor() {
        return acceptor;
    }

    public boolean isOnline() {
        return online;
    }

    public List<Pair<Integer, String>> worldRecommendedList() {
        return worldRecommendedList;
    }

    public void removeChannel(int worldid, int channel) {
        channels.remove(channel);

        World world = worlds.get(worldid);
        if (world != null) {
            world.removeChannel(channel);
        }
    }

    public Channel getChannel(int world, int channel) {
        return worlds.get(world).getChannel(channel);
    }

    public List<Channel> getChannelsFromWorld(int world) {
        return worlds.get(world).getChannels();
    }

    public List<Channel> getAllChannels() {
        List<Channel> channelz = new ArrayList<>();
        for (World world : worlds) {
            channelz.addAll(world.getChannels());
        }
        return channelz;
    }

    public String getIP(int world, int channel) {
        return channels.get(world).get(channel);
    }

    @Override
    public void run() {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("world.ini"));
        } catch (Exception e) {
            LOGGER.info("Please start create_server.bat");
            System.exit(0);
        }

        LOGGER.info("LucianMS v{} starting up!", ServerConstants.VERSION);

        try {
            if (Defaults.createDefaultIfAbsent(null, "server-config.json")) {
                LOGGER.info("Server config created. Configure settings and restart the server");
                System.exit(0);
                return;
            } else {
                config = new Config(new JSONObject(new JSONTokener(new FileInputStream("server-config.json"))));
            }

            if (config.getBoolean("WhitelistEnabled")) {
                if (Defaults.createDefaultIfAbsent(null, "whitelist.json")) {
                    LOGGER.info("Whitelist file created");
                }
                Whitelist.loadAccounts();
                LOGGER.info(Whitelist.getAccounts().size() + " whitelisted accounts loaded");
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        DiscordSession.listen();

        TaskExecutor.prestartAllCoreThreads();
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown()));

        try {
            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());
            acceptor = new NioSocketAcceptor();
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 120);
            acceptor.setHandler(new MapleServerHandler());
            acceptor.bind(new InetSocketAddress(8484));
            LOGGER.info("Listening on port 8484");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
            return;
        }

        DatabaseConnection.useConfig(config);
        Connection con = DatabaseConnection.getConnection();
        LOGGER.info("Database connection established");
        try {
            con.createStatement().execute("UPDATE accounts SET loggedin = 0");
            con.createStatement().execute("UPDATE characters SET hasmerchant = 0");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        TaskExecutor.createRepeatingTask(RankingWorker::new, ServerConstants.RANKING_INTERVAL);

        // schedule a daily task
        Calendar tmrw = Calendar.getInstance();
        tmrw.set(Calendar.DATE, tmrw.get(Calendar.DATE) + 1);
        tmrw.set(Calendar.HOUR_OF_DAY, 2);
        tmrw.set(Calendar.MINUTE, 0);
        tmrw.set(Calendar.SECOND, 0);
        tmrw.set(Calendar.MILLISECOND, 0);
        TaskExecutor.createRepeatingTask(new DailyWorker(), (tmrw.getTimeInMillis() - System.currentTimeMillis()), TimeUnit.DAYS.toMillis(1));
        LOGGER.info("Entry reset scheduled to run in {}", StringUtil.getTimeElapse(tmrw.getTimeInMillis() - System.currentTimeMillis()));

        long timeToTake = System.currentTimeMillis();
        MapleQuest.loadAllQuest();
        CQuestBuilder.loadAllQuests();
        LOGGER.info("Quest data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        timeToTake = System.currentTimeMillis();
        SkillFactory.loadAllSkills();
        LOGGER.info("Skill data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        timeToTake = System.currentTimeMillis();
        MapleItemInformationProvider.getInstance().getAllItems();
        LOGGER.info("Item data loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));
        CashItemFactory.loadCommodities();
        LOGGER.info("Cash shop commodities loaded in {}s", ((System.currentTimeMillis() - timeToTake) / 1000d));

        Achievements.loadAchievements();

        timeToTake = System.currentTimeMillis();
        int count = HouseManager.loadHouses();
        LOGGER.info("{} houses loaded in {}s", count, ((System.currentTimeMillis() - timeToTake) / 1000d));

        try {
            timeToTake = System.currentTimeMillis();
            int qWorlds = Integer.parseInt(p.getProperty("worlds"));
            for (int i = 0; i < qWorlds; i++) {
                int flag = Integer.parseInt(p.getProperty("flag" + i));
                String eMessage = p.getProperty("eventmessage" + i);
                String sMessage = p.getProperty("servermessage" + i);

                World world = new World(i, flag, eMessage, Integer.parseInt(p.getProperty("exprate" + i)), Integer.parseInt(p.getProperty("droprate" + i)), Integer.parseInt(p.getProperty("mesorate" + i)), Integer.parseInt(p.getProperty("bossdroprate" + i)));

                worlds.add(world);
                worldRecommendedList.add(new Pair<>(i, p.getProperty("recommend" + i)));

                channels.add(new LinkedHashMap<>());
                int qChannels = Integer.parseInt(p.getProperty("channels" + i));
                for (int j = 0; j < qChannels; j++) {
                    int channelId = j + 1;
                    Channel channel = new Channel(i, channelId);
                    world.addChannel(channel);
                    channels.get(i).put(channelId, channel.getIP());
                }
                world.setServerMessage(sMessage);

                //                final long repeat = (1000 * 60 * 60) * 4;
                //                TaskExecutor.createRepeatingTask(() -> GAutoEventManager.startRandomEvent(world), repeat);
                world.addScheduledEvent(new SOuterSpace(world));
                LOGGER.info("World {} created in {}s", (world.getId() + 1), ((System.currentTimeMillis() - timeToTake) / 1000d));
            }
        } catch (Exception e) {
            e.printStackTrace();// For those who get errors
            System.exit(0);
        }

        ConsoleCommands.beginReading();
        LOGGER.info("Console now listening for commands");

        LOGGER.info("LucianMS took {}s to start", ((System.currentTimeMillis() - timeToTake) / 1000d));
        online = true;

    }

    public Properties getSubnetInfo() {
        return subnetInfo;
    }

    public Config getConfig() {
        return config;
    }

    public MapleAlliance getAlliance(int id) {
        synchronized (alliances) {
            if (alliances.containsKey(id)) {
                return alliances.get(id);
            }
            return null;
        }
    }

    public void addAlliance(int id, MapleAlliance alliance) {
        synchronized (alliances) {
            if (!alliances.containsKey(id)) {
                alliances.put(id, alliance);
            }
        }
    }

    public void disbandAlliance(int id) {
        synchronized (alliances) {
            MapleAlliance alliance = alliances.get(id);
            if (alliance != null) {
                for (Integer gid : alliance.getGuilds()) {
                    guilds.get(gid).setAllianceId(0);
                }
                alliances.remove(id);
            }
        }
    }

    public void allianceMessage(int id, final byte[] packet, int exception, int guildex) {
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

    public boolean addGuildtoAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.addGuild(guildId);
            return true;
        }
        return false;
    }

    public boolean removeGuildFromAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.removeGuild(guildId);
            return true;
        }
        return false;
    }

    public boolean setAllianceRanks(int aId, String[] ranks) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setRankTitle(ranks);
            return true;
        }
        return false;
    }

    public boolean setAllianceNotice(int aId, String notice) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setNotice(notice);
            return true;
        }
        return false;
    }

    public boolean increaseAllianceCapacity(int aId, int inc) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.increaseCapacity(inc);
            return true;
        }
        return false;
    }

    public Set<Integer> getChannelServer(int world) {
        return new HashSet<>(channels.get(world).keySet());
    }

    public byte getHighestChannelId() {
        byte highest = 0;
        for (Iterator<Integer> it = channels.get(0).keySet().iterator(); it.hasNext(); ) {
            Integer channel = it.next();
            if (channel != null && channel > highest) {
                highest = channel.byteValue();
            }
        }
        return highest;
    }

    public int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }

    public MapleGuild getGuild(int id, int world, MapleGuildCharacter mgc) {
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

    public void clearGuilds() {// remake
        synchronized (guilds) {
            guilds.clear();
        }
        // for (List<Channel> world : worlds.values()) {
        // reloadGuildCharacters();
    }

    public void setGuildMemberOnline(MapleGuildCharacter mgc, boolean bOnline, int channel) {
        MapleGuild g = getGuild(mgc.getGuildId(), mgc.getWorld(), mgc);
        g.setOnline(mgc.getId(), bOnline, channel);
    }

    public int addGuildMember(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mgc);
        }
        return 0;
    }

    public boolean setGuildAllianceId(int gId, int aId) {
        MapleGuild guild = guilds.get(gId);
        if (guild != null) {
            guild.setAllianceId(aId);
            return true;
        }
        return false;
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public void guildChat(int gid, String name, int cid, String msg) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public void changeRank(int gid, int cid, int newRank) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRank(cid, newRank);
        }
    }

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        MapleGuild g = guilds.get(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public void setGuildNotice(int gid, String notice) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mgc);
        }
    }

    public void changeRankTitle(int gid, String[] ranks) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public void disbandGuild(int gid) {
        synchronized (guilds) {
            MapleGuild g = guilds.get(gid);
            g.disbandGuild();
            guilds.remove(gid);
        }
    }

    public boolean increaseGuildCapacity(int gid) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public void gainGP(int gid, int amount) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public void guildMessage(int gid, byte[] packet) {
        guildMessage(gid, packet, -1);
    }

    public void guildMessage(int gid, byte[] packet, int exception) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.broadcast(packet, exception);
        }
    }

    public PlayerBuffStorage getPlayerBuffStorage() {
        return buffStorage;
    }

    public void deleteGuildCharacter(MapleGuildCharacter mgc) {
        setGuildMemberOnline(mgc, false, (byte) -1);
        if (mgc.getGuildRank() > 1) {
            leaveGuild(mgc);
        } else {
            disbandGuild(mgc.getGuildId());
        }
    }

    public void reloadGuildCharacters(int world) {
        World worlda = getWorld(world);
        for (MapleCharacter mc : worlda.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildId() > 0) {
                setGuildMemberOnline(mc.getMGC(), true, worlda.getId());
                memberLevelJobUpdate(mc.getMGC());
            }
        }
        worlda.reloadGuildSummary();
    }

    public void broadcastMessage(final byte[] packet) {
        for (Channel ch : getChannelsFromWorld(0)) {
            ch.broadcastPacket(packet);
        }
    }

    public void broadcastGMMessage(final byte[] packet) {
        for (Channel ch : getChannelsFromWorld(0)) {
            ch.broadcastGMPacket(packet);
        }
    }

    public boolean isGmOnline() {
        for (Channel ch : getChannelsFromWorld(0)) {
            for (MapleCharacter player : ch.getPlayerStorage().getAllCharacters()) {
                if (player.isGM()) {
                    return true;
                }
            }
        }
        return false;
    }

    public World getWorld(int id) {
        return worlds.get(id);
    }

    public List<World> getWorlds() {
        return worlds;
    }

    private Runnable shutdown() {// only once :D
        return new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Shutdown hook invoked");

                acceptor.unbind();
                acceptor.dispose();
                acceptor = null;
                LOGGER.info("Login server closed");

                getWorlds().forEach(World::shutdown);

                TaskExecutor.shutdownNow();

                LOGGER.info("Worlds & channels are now offline");

                ConsoleCommands.stopReading();

                DiscordSession.ignore();
            }
        };
    }
}