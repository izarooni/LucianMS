package net.discord.handlers;

/**
 * A shitty attempt on having two JVM's communicate locally
 *
 * @author izarooni
 */
public class DiscordRequestManager {

    private static DiscordRequest[] requests;

    static {
        requests = new DiscordRequest[3];
        requests[0] = new ShutdownRequest();
        requests[1] = new FaceChangeRequest();
        requests[2] = new HairChangeRequest();
    }

    private DiscordRequestManager() {
    }

    public static DiscordRequest getRequest(int i) {
        return requests[i];
    }
}
