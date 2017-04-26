package client.autoban;

import java.util.HashMap;

import client.MapleClient;
import discord.Discord;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

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

        public void announce(final MapleClient client, String message, int cooldown) {
            if (System.currentTimeMillis() - latestAnnouncement < cooldown) {
                return;
            }
            try {
                String channel = Discord.getConfig().getString("cheaterChannel");
                if (channel != null) {
                    new MessageBuilder(Discord.getBot().getClient()).withChannel(channel).appendContent(message).build();
                } else {
                    System.err.println("No discord channel set to send cheater messages! Edit Discord config file ASAP");
                }
            } catch (RateLimitException | MissingPermissionsException | DiscordException e) {
                System.err.println(String.format("Unable to send %s's cheater message ('%s') due to error: %s", client.getPlayer().getName(), message, e.getMessage()));
            }
            latestAnnouncement = System.currentTimeMillis();
        }
    }

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
