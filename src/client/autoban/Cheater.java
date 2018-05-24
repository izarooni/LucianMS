package client.autoban;

import client.MapleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author izarooni
 */
public class Cheater {

    public static class CheatEntry {

        public int cheatCount = 0; // amount of cheat operations
        public long latestCheatTimestamp = 0; // last recorded time of cheat operation

        public int spamCount = 0; // amount of times an operation was executed too fast
        public long latestOperationTimestamp = 0; // last recorded time of cheat operation

        private long latestAnnouncement = 0; // prevents spam messages

        public void incrementCheatCount() {
            cheatCount++;
            latestCheatTimestamp = System.currentTimeMillis();
        }

        public void announce(final MapleClient client, int cooldown, String message, Object... args) {
            if (System.currentTimeMillis() - latestAnnouncement < cooldown) {
                return;
            }
            LOGGER.error(message, args);
//            if (!client.getPlayer().isGM()) {
//            }
            latestAnnouncement = System.currentTimeMillis();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Cheater.class);
    private HashMap<Cheats, CheatEntry> convicts = new HashMap<>();

    public Cheater() {
        for (Cheats cheats : Cheats.values()) {
            convicts.put(cheats, new CheatEntry());
        }
    }

    public CheatEntry getCheatEntry(Cheats cheat) {
        return convicts.get(cheat);
    }
}
