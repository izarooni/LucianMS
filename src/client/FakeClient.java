package client;

/**
 * Override {@link MapleClient} methods to prevent stupid exceptions...
 * <p>
 * A {@code FakeClient} (linked to a {@link server.life.FakePlayer}) must never receive packet information,
 * as there is no need to send information to a non-existent client. A FakePlayer will always follow it's
 * leader, thus copying receiving information from an actual user
 * </p>
 *
 * @author izarooni
 */
public class FakeClient extends MapleClient {


    public FakeClient() {
        super(null, null, null);
    }

    @Override
    public synchronized void announce(byte[] packet) {
        // do nothing
    }
}
