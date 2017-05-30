package discord;


import discord.commands.CommandHelper;
import discord.commands.CommandManagerHelper;
import discord.io.Config;
import discord.io.defaults.Defaults;
import discord.lang.DuplicateEntryException;
import org.json.JSONObject;
import org.json.JSONTokener;
import sx.blah.discord.util.DiscordException;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Ian
 */
public class Discord {

    private static final Discord INSTANCE = new Discord();
    private static final Bot bot = new Bot();
    private static Config config;

    private Discord() {
    }

    public static Discord getInstance() {
        return INSTANCE;
    }

    public static Bot getBot() {
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
                Defaults.createDefault("discord/", "config.json");
                println("Created config file... Make changes then restart");
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        LoadExternalCommands();

        try {
            config = new Config(new JSONObject(new JSONTokener(new FileInputStream("discord/config.json"))));
            println("Config file loaded");
            Discord.getBot().login();
            println("Now online Discord");
            return true;
        } catch (LoginException | InterruptedException | FileNotFoundException | DiscordException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This does not dispose the already existing command managers
     */
    public static void LoadExternalCommands() {
        //region loading command managers from external jars
        try {
            File cmds = new File("discord/commands");
            if (cmds.mkdirs()) {
                println("External command management directory created");
            }
            try {
                File[] files = cmds.listFiles();
                if (files != null) {
                    System.out.print("Loading command managers");
                    for (File file : files) {
                        JarFile jar = new JarFile(file);
                        ZipEntry zip = jar.getEntry("info.ini");
                        if (zip == null) {
                            System.err.println("Command manager contains invalid info file: " + file.getName());
                            continue;
                        }
                        try (InputStream in = jar.getInputStream(zip)) {
                            Properties props = new Properties();
                            props.load(in); // load info file properties

                            String name = (String) props.get("name");
                            String main = (String) props.get("main");

                            if (CommandManagerHelper.getCommandManager(name) != null) {
                                in.close();
                                jar.close();
                                throw new DuplicateEntryException(String.format("Command processor with name %s already exists", name));
                            }

                            URL[] urls = Collections.singletonList(file.toURI().toURL()).toArray(new URL[1]);
                            URLClassLoader loader = new URLClassLoader(urls);
                            // info properties must contain the package path to the CommandHelper sub-class
                            Class toLoad = Class.forName(main, true, loader);
                            CommandHelper helper = (CommandHelper) toLoad.newInstance();
                            helper.onLoad();

                            CommandManagerHelper.addCommandManager(name, loader, helper);
                            System.out.print(".");
                        }
                    }
                    System.out.println();
                    println("Loaded " + CommandManagerHelper.getManagers().size() + " command managers!");
                }
            } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                System.err.println("Was unable to load external commands: " + e.getMessage());
            }
        } catch (SecurityException e) {
            System.err.println("Was unable to create a folder for command managements: " + e.getMessage());
        }
        //endregion
    }
}