package constants;

public class ServerConstants {

	public static short VERSION = 83;
	public static String[] WORLD_NAMES = { "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia",
			"Yellonde", "Demethos", "Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis", "Plana", "Kalluna",
			"Stius", "Croa", "Medere" };

	// Login Configuration
	public static final int CHANNEL_LOAD = 100;// Players per channel
	public static final long RANKING_INTERVAL = 60 * 60 * 1000;// 60 minutes,
																// 3600000
	public static final boolean ENABLE_PIC = true;

	// Event Configuration
	public static final boolean PERFECT_PITCH = false;

	// Game play Configurations
	public static final boolean USE_MTS = false;
	public static final boolean USE_FAMILY_SYSTEM = false;
	public static final boolean USE_DUEY = false;
	public static final boolean USE_ITEM_SORT = true;
	public static final boolean USE_PARTY_SEARCH = false;
	public static final int  HOME_MAP = 820000000;
	public static final int CURRENCY = 4260002;

	// Rates
	public static final int PARTY_EXPERIENCE_MOD = 1; // change for event stuff
	public static final double PQ_BONUS_EXP_MOD = 0.5;

	public static final long EVENT_END_TIMESTAMP = 1428897600000L;

	// Gain nx from monsters
	public static final boolean NX_FROM_MONSTERS = true;

	public static final int BELOW_LEVERANGEL_NX_CHANCE = 20;
	public static final int ABOVE_LEVELRANGE_NX_CHANCE = 10;

	public static final int MAX_LEVELS_BELOW = 20;
	public static final int MAX_LEVELS_ABOVE = 10;

	public static final double LEVEL_TO_NX_MULTIPLIER = 1.2;
}
