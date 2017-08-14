package net.discord.handlers;

/**
 * A shitty attempt on having two external JVM's communicate locally
 *
 * @author izarooni
 */
public class DiscordRequests {

    private static DiscordRequest[] requests;

    static {
        requests = new DiscordRequest[3];
        requests[0] = new ShutdownRequest();
        requests[1] = new FaceChangeRequest();
        requests[2] = new HairChangeRequest();
    }

    private DiscordRequests() {
    }

    public static DiscordRequest getRequest(int i) {
        return requests[i];
    }
}
