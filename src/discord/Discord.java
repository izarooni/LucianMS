package discord;


import discord.commands.data.DCommandManager;
import io.Config;
import io.defaults.Defaults;
import discord.user.DiscordUser;
import org.json.JSONObject;
import org.json.JSONTokener;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class Discord {

    private static final Discord INSTANCE = new Discord();
    private static final DiscordBot bot = new DiscordBot();
    private static Config config;
    private static ConcurrentHashMap<Long, DiscordGuild> guilds = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, DiscordUser> users = new ConcurrentHashMap<>();

    private static DCommandManager commandManager = new DCommandManager();

    private Discord() {
    }

    public static Discord getInstance() {
        return INSTANCE;
    }

    public static DiscordBot getBot() {
        return bot;
    }

    public static Config getConfig() {
        return config;
    }

    public static void println(String s) {
        System.out.println(String.format("[Discord] %s", s));
    }

    public static boolean initialize() {
        File fConfig = new File("discord/config.json");
        if (!fConfig.exists()) {
            try {
                Defaults.createDefault("discord", "discord-config.json");
                println("Discord config created. Configure settings and restart");
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        LoadPermissions();

        try {
            config = new Config(new JSONObject(new JSONTokener(new FileInputStream("discord/discord-config.json"))));
            println("Config file loaded");
            Discord.getBot().login();
            println("Now online Discord");
            return true;
        } catch (LoginException | InterruptedException | FileNotFoundException | DiscordException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static DiscordUser getUser(IUser user) {
        if (!users.containsKey(user.getLongID())) {
            DiscordUser du = new DiscordUser(user);
            users.put(user.getLongID(), du);
            return du;
        }
        return users.get(user.getLongID());
    }

    public static DiscordGuild getGuild(IGuild guild) {
        if (!guilds.containsKey(guild.getLongID())) {
            DiscordGuild dg = new DiscordGuild(guild);
            guilds.put(guild.getLongID(), dg);
            return dg;
        }
        return guilds.get(guild.getLongID());
    }

    public static DCommandManager getCommandManager() {
        return commandManager;
    }

    private static void LoadPermissions() {
        File cmds = new File("discord/permissions");
        try {
            if (cmds.mkdirs()) {
                println("External command management directory created");
            }
        } catch (SecurityException e) {
            System.err.println("Was unable to create a folder for command managements: " + e.getMessage());
        }
    }
}
