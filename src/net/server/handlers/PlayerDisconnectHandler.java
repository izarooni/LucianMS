package net.server.handlers;

import net.PacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * Created for method handles
 *
 * @author izarooni
 */
public class PlayerDisconnectHandler extends PacketHandler {

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        // nothing to prccess
    }

    @Override
    public void onPacket() {
    }
}
