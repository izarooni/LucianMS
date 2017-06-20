package discord.commands.data;

/**
 * @author izarooni
 */
public enum DUserPower {

    Stylist, Trader, ClearInv, Strip, ReloadMap, Warp, Permissions;

    @Override
    public String toString() {
        return String.format("%s.%s", getClass().getSimpleName(), name());
    }

    public static boolean isValidPermission(String permission) {
        for (DUserPower cmd : values()) {
            if (cmd.toString().equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
