package net;

import client.MapleClient;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * Process packets so the data can be used in other ways then server handles (e.g. auto events, PQs, etc.)
 * <p>
 * Should they need to be canceled (not proceed to server handles), the other non-server handles can use the processed data and act accordingly
 * </p>
 *
 * @author izarooni
 */
public abstract class PacketHandler {

    private MapleClient client;
    private boolean canceled = false;

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

    public abstract void process(SeekableLittleEndianAccessor slea);

    public abstract void onPacket();
}
