package com.lucianms.constants;

import java.util.Arrays;
import java.util.List;

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

    public static final int[] NO_SKILL_MAPS = {910000000, 109020001};
    public static final int HOME_MAP = 910000000;
    public static final int CURRENCY = 4260002;
    public static final int BOMB_MOB= 9300166;
    public static final List<Integer> bannedItems = Arrays.asList(1832000, 1822000, 1812007);
    public static final List<Integer> WizetItems = Arrays.asList(1002140, 1003142, 1042003, 1062007, 1322013);
    public static final int JAIL = 80;

    public static final boolean NX_FROM_MONSTERS = false;
    public static final double LEVEL_TO_NX_MULTIPLIER = 1.2;
    public static final int BELOW_LEVERANGEL_NX_CHANCE = 20;
    public static final int ABOVE_LEVELRANGE_NX_CHANCE = 10;
    public static final int MAX_LEVELS_BELOW = 20;
    public static final int MAX_LEVELS_ABOVE = 10;
    public static final short TIER1 = 14000;
    public static final short TIER2 = 21000;
    public static final short TIER3 = 27000;
    public static final short TIER4 = 32700;
    public static final int aTIER1 = 50;
    public static final int aTIER2 = 75;
    public static final int aTIER3 = 100;
    public static final int aTIER4 = 150;

    public static int getAutoRebirthItem() {
        // using a method for when we decide to make an item cycle (different daily)
        return 4011008;
    }
    //endregion
}
