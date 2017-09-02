package net.discord.handlers;

import net.discord.Headers;

/**
 * A shitty attempt on having two JVM's communicate locally
 *
 * @author izarooni
 */
public class DiscordRequestManager {

    private static DiscordRequest[] requests = new DiscordRequest[Headers.values().length];

    static {
        requests[Headers.Shutdown.value] = new ShutdownRequest();
        requests[Headers.SetFace.value] = new FaceChangeRequest();
        requests[Headers.SetHair.value] = new HairChangeRequest();
        requests[Headers.Online.value] = new OnlineRequest();
    }

    private DiscordRequestManager() {
    }

    public static DiscordRequest getRequest(int i) {
        return requests[i];
    }
}
