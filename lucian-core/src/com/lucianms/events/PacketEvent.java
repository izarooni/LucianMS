package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.util.ArrayDeque;

/**
 * Process packets so the data can be used in other ways then server handles (e.g. auto events, PQs, etc.)
 * <p>
 * Should they need to be canceled (not proceed to server handles), the other non-server handles can use the processed data and act accordingly
 * </p>
 *
 * @author izarooni
 */
public abstract class PacketEvent implements Cleaner.Cleanable {

    private Logger LOGGER = null;
    private MapleClient client;
    private boolean canceled = false;
    private ArrayDeque<Runnable> posts = new ArrayDeque<>(3);

    public final Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = LoggerFactory.getLogger(getClass());
        }
        return LOGGER;
    }

    public final void setClient(MapleClient client) {
        this.client = client;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final boolean isCanceled() {
        return canceled;
    }

    public final void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public final void onPost(Runnable runnable) {
        posts.add(runnable);
    }

    public  void packetCompleted() {
        Runnable poll;
        while ((poll = posts.poll()) != null) {
            poll.run();
        }
        clean();
    }

    @Override
    public void clean() {
    }

    public boolean inValidState() {
        return client.isLoggedIn();
    }

    public void exceptionCaught(Throwable t) {
        t.printStackTrace();
    }

    public abstract void processInput(MaplePacketReader reader);

    public abstract Object onPacket();
}
