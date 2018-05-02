package com.lucianms.discord.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * <p>
 * Socket should only be used locally and packet shouldn't be carrying
 * any sensitive information that would need encrypting/decrypting
 * </p>
 *
 * @author izarooni
 */
public class RawDecoder extends CumulativeProtocolDecoder {

    @Override
    protected boolean doDecode(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
        byte[] packet = new byte[buffer.remaining()];
        buffer.get(packet, 0, packet.length);
        out.write(packet);
        return true;
    }
}
