package com.lucianms.constants;

/**
 * @author izarooni
 */
public class ServerConstants {

    public static short VERSION = 83;
    public static String[] WORLD_NAMES = {"Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia",
            "Yellonde", "Demethos", "Galicia", "El Nido", "Zenith", "Arcenia", "Kastia", "Judis", "Plana", "Kalluna",
            "Stius", "Croa", "Medere"};

    private ServerConstants() {
    }

    public static class MAPS {
        public static final int CharacterCreation = 10000;
        public static final int Home = 910000000;

        private MAPS() {
        }
    }

    public static class LOGIN {
        /**
         * A.K.A. PIC; Character selection password
         */
        public static boolean EnableSPW = false;
        /**
         * Should the server automatically register accounts if they don't exist
         */
        public static boolean EnableAutoRegister = true;
        public static final int ChannelUserCapacity = 100;

        private LOGIN() {
        }
    }

    public static class GAME {
        /**
         * <p>
         * 0    : party bonus<br>
         * 100  : 1x bonus<br>
         * 200  : 2x bonus<br>
         * </p>
         */
        public static final int BonusPartyExp = 100;
        public static final int SoftCurrency = 4260002;
        public static final int BombMonster = 9300166;
        public static boolean SaveCharacterSkills = false;
        public static boolean EnableFamilies = false;
        public static boolean EnablePartySearch = false;

        private GAME() {
        }
    }

    public static int getAutoRebirthItem() {
        // using a method for when we decide to make an item cycle (different daily)
        return 4011008;
    }
}
