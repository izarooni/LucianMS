package com.lucianms.client.autoban;

import com.lucianms.client.MapleClient;
import com.lucianms.discord.DiscordConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;

/**
 * @author izarooni
 */
public class Cheater {

    public static class CheatEntry {

        private int cheatCount = 0; // amount of cheat operations
        private long latestCheatTimestamp = 0; // last recorded time of cheat operation

        private long latestAnnouncement = 0; // prevents spam messages

        public int getCheatCount() {
            return cheatCount;
        }

        public void resetCheatCount() {
            cheatCount = 0;
        }

        public boolean testFor(long cooldown) {
            return System.currentTimeMillis() - latestCheatTimestamp <= cooldown;
        }

        public void record() {
            cheatCount++;
            latestCheatTimestamp = System.currentTimeMillis();
        }

        public void announce(final MapleClient client, int cooldown, String message, Object... args) {
            if (System.currentTimeMillis() - latestAnnouncement < cooldown) {
                return;
            }
            if (!client.getPlayer().isGM()) {
                String content = MessageFormatter.arrayFormat(message, args).getMessage();
                LOGGER.info(content);
                DiscordConnection.sendMessage(502056472461443072L, content);
            }
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
