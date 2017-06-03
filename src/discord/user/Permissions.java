package discord.user;

import discord.commands.CommandHelper;
import discord.commands.CommandManagerHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author izarooni
 */
public class Permissions {

    private Permissions() {
    }

    private static boolean invalidPermission(String permission) {
        if (permission.equals("*")) {
            return false;
        }
        boolean t = false;
        for (CommandHelper helper : CommandManagerHelper.getManagers()) {
            if (!helper.isValidPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public static void setPermission(DUser user, String permission) throws IOException {
        if (invalidPermission(permission)) {
            return;
        }
        final String path = "discord/permissions/" + user.getUser().getStringID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            if (file.createNewFile()) {
                file = new File(path);
            } else {
                throw new RuntimeException("Unable to create data file for user " + user.getUser().getStringID() + " '" + user.getUser().getName() + "'");
            }
        }
        boolean save;
        if (permission.equals("*")) {
            for (CommandHelper helper : CommandManagerHelper.getManagers()) {
                helper.getPermissions().forEach(user::givePermission);
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
        }
    }

    public static void removePermission(DUser user, String permission) throws IOException {
        if (invalidPermission(permission)) {
            return;
        }
        final String path = "discord/permissions/" + user.getUser().getStringID() + ".json";
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        boolean save;
        if (permission.equals("*")) {
            for (CommandHelper helper : CommandManagerHelper.getManagers()) {
                helper.getPermissions().forEach(user::removePermission);
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
