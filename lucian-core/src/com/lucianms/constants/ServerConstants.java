package com.lucianms.constants;

public class ServerConstants {

    public static short VERSION = 83;
    public static String[] WORLD_NAMES = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia",
            "Yellonde", "Demethos", "Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis", "Plana", "Kalluna",
            "Stius", "Croa", "Medere"};

    //region Login Settings
    /**
     * the player capacity per channel
     */
    public static final int CHANNEL_LOAD = 100;

    /**
     * should the SPW be enabled upon character selection
     */
    public static final boolean ENABLE_PIC = true;
    //endregion

    //region Game Settings
    public static final boolean SAVE_CHARACTER_SKILLS = false;
    public static final boolean USE_FAMILY_SYSTEM = false;
    public static final boolean USE_PARTY_SEARCH = false;

    public static final int PARTY_EXPERIENCE_MOD = 1;

    public static final int HOME_MAP = 910000000;
    public static final int CURRENCY = 4260002;

    public static final boolean NX_FROM_MONSTERS = false;
    public static final double LEVEL_TO_NX_MULTIPLIER = 1.2;
    public static final int BELOW_LEVERANGEL_NX_CHANCE = 20;
    public static final int ABOVE_LEVELRANGE_NX_CHANCE = 10;
    public static final int MAX_LEVELS_BELOW = 20;
    public static final int MAX_LEVELS_ABOVE = 10;

    public static int getAutoRebirthItem() {
        // using a method for when we decide to make an item cycle (different daily)
        return 4011008;
    }
    //endregion
}
