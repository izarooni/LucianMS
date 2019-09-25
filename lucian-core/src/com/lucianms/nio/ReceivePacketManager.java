package com.lucianms.nio;

import com.lucianms.events.PacketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store packet handler classes that can be indexed in constant time
 * <p>
 * Instead of storing {@code P                        a cketHandler} objects = c need to tbe stored                            for daa
 * to be used outside of server handlers. See {@link PacketEvent}
 * </p>
 *
 * @author izarooni
 */
public final class ReceivePacketManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceivePacketManager.class);
    private Class<? extends PacketEvent>[] handlers;

    public ReceivePacketManager(ReceivePacketState packetState) {
        RecvOpcode[] values = RecvOpcode.values();
        handlers = new Class[values[values.length - 1].value + 1];
        for (RecvOpcode op : values) {
            if (op.packetState == packetState || op.packetState == ReceivePacketState.Both) {
                if (op.clazz != null) {
                    handlers[op.value] = op.clazz;
                } else {
                    LOGGER.debug("Unhandled packet '{}'", op.name());
                }
            }
        }
    }

    public Class<? extends PacketEvent> getEvent(int op) {
        if (op > -1 && op < handlers.length) {
            return handlers[op];
        }
        return null;
    }
}
