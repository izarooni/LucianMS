package net.discord.handlers;

import net.discord.Headers;

/**
 * A shitty attempt on having two JVM's communicate locally
 *
 * @author izarooni
 */
public class DiscordRequestManager {

    private static DiscordRequest[] requests;

    static {
        requests = new DiscordRequest[Headers.values().length];
        requests[0x0] = new ShutdownRequest();
        requests[0x1] = new FaceChangeRequest();
        requests[0x2] = new HairChangeRequest();
        requests[0x3] = new OnlineRequest();
    }

    private DiscordRequestManager() {
    }

    public static DiscordRequest getRequest(int i) {
        return requests[i];
    }
}
