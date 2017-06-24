package discord.user;

import discord.Discord;
import discord.DiscordGuild;
import discord.commands.data.DUserPower;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author izarooni
 */
public final class Permissions {

    private Permissions() {
        File file;

        file = new File("discord/permissions");
        if (file.mkdirs()) {
            Discord.println("Permissions folder created");
        }

        file = new File(file, "guilds");
        if (file.mkdirs()) {
            Discord.println("Guild permissions folder created");
        }
    }

    public static boolean invalidPermission(String permission) {
        if (permission.equals("*")) {
            return false;
        }
        return DUserPower.isValidPermission(permission);
    }

    public static ArrayList<String> load(DiscordGuild guild) throws IOException {
        ArrayList<String> arrayList = new ArrayList<>();

        final String path = "discord/permissions/guilds/" + guild.getGuild().getLongID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            if (file.createNewFile()) {
                // no permissions -- nothing to load
                return arrayList;
            } else {
                throw new RuntimeException("Unable to create data file for user " + guild.getGuild().getLongID() + " '" + guild.getGuild().getName() + "'");
            }
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            JSONObject object = new JSONObject(new JSONTokener(fis));
            JSONObject perms = object.getJSONObject("permissions");
            Iterator<String> keys = perms.keys();
            while (keys.hasNext()) {
                String role = keys.next();
                JSONArray array = perms.getJSONArray(role);
                for (int i = 0; i < array.length(); i++) {
                    Object o = array.get(i);
                    guild.givePermission(role, (String) o);
                }
            }
        }
        return arrayList;
    }

    public static ArrayList<String> load(DiscordUser user) throws IOException {
        ArrayList<String> arrayList = new ArrayList<>();

        final String path = "discord/permissions/" + user.getUser().getLongID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            if (file.createNewFile()) {
                // no permissions -- nothing to load
                return arrayList;
            } else {
                throw new RuntimeException("Unable to create data file for user " + user.getUser().getLongID() + " '" + user.getUser().getName() + "'");
            }
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            JSONObject object = new JSONObject(new JSONTokener(fis));
            JSONArray array = object.getJSONArray("permissions");
            for (int i = 0; i < array.length(); i++) {
                Object o = array.get(i);
                user.givePermission((String) o);
            }
        }

        return arrayList;
    }

    public static void serverPermission(DiscordGuild guild, String role, String permission, boolean add) throws IOException {
        final String path = "discord/permissions/guilds/" + guild.getGuild().getLongID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            if (file.createNewFile()) {
                file = new File(path);
            } else {
                throw new RuntimeException("Unable to create data file for guild " + guild.getGuild().getLongID() + " '" + guild.getGuild().getName() + "'");
            }
        }
        boolean save;
        if (permission.equals("*")) {
            for (DUserPower userPower : DUserPower.values()) {
                if (add) {
                    guild.givePermission(role, userPower.toString());
                } else {
                    guild.removePermission(role, userPower.toString());
                }
            }
            save = true;
        } else {
            if (add) {
                save = guild.givePermission(role, permission);
            } else {
                save = guild.removePermission(role, permission);
            }
        }

        if (save) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(guild.toJSON().toString().getBytes());
                fos.flush();
            }
        } else {
            System.err.println(String.format("Guild(%s) permission(%s) not changed", guild.getGuild().getName(), permission));
        }
    }

    public static void setPermission(DiscordUser user, String permission) throws IOException {
        if (invalidPermission(permission)) {
            return;
        }
        final String path = "discord/permissions/" + user.getUser().getLongID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            if (file.createNewFile()) {
                file = new File(path);
            } else {
                throw new RuntimeException("Unable to create data file for user " + user.getUser().getLongID() + " '" + user.getUser().getName() + "'");
            }
        }
        boolean save;
        if (permission.equals("*")) {
            for (DUserPower userPower : DUserPower.values()) {
                user.givePermission(userPower.toString());
            }
            save = true;
        } else {
            save = user.givePermission(permission);
        }

        if (save) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(user.toJSON().toString().getBytes());
                fos.flush();
            }
        } else {
            System.err.println(String.format("Yser(%s) permission(%s) not changed", user.getUser().getName(), permission));

        }
    }

    public static void removePermission(DiscordUser user, String permission) throws IOException {
        if (invalidPermission(permission)) {
            return;
        }
        final String path = "discord/permissions/" + user.getUser().getLongID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        boolean save;
        if (permission.equals("*")) {
            for (DUserPower userPower : DUserPower.values()) {
                user.givePermission(userPower.toString());
            }
            save = true;
        } else {
            save = user.removePermission(permission);
        }
        if (save) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(user.toJSON().toString().getBytes());
                fos.flush();
            }
        }
    }
}
