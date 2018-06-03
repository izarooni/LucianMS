package net;

import client.MapleClient;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayDeque;

/**
 * Process packets so the data can be used in other ways then server handles (e.g. auto events, PQs, etc.)
 * <p>
 * Should they need to be canceled (not proceed to server handles), the other non-server handles can use the processed data and act accordingly
 * </p>
 *
 * @author izarooni
 */
public abstract class PacketEvent {

    private MapleClient client;
    private boolean canceled = false;
    private ArrayDeque<Runnable> posts = new ArrayDeque<>(3);

    final void setClient(MapleClient client) {
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

    public boolean inValidState() {
        return client.isLoggedIn();
    }

    public void exceptionCaught(Throwable t) {
        t.printStackTrace();
    }

    public void onPost(Runnable runnable) {
        posts.add(runnable);
    }

    public void post() {
        Runnable poll;
        while ((poll = posts.poll()) != null) {
            poll.run();
        }
    }

    public abstract void process(SeekableLittleEndianAccessor slea);

    public abstract Object onPacket();
}
