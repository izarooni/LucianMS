package net.discord.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * <p>
 * Socket should only be used locally and packet shouldn't be carrying
 * any sensitive information that would need encrypting/decrypting
 * </p>
 *
 * @author izarooni
 */
public class RawEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession ioSession, Object object, ProtocolEncoderOutput out) throws Exception {
        byte[] packet = (byte[]) object;
        out.write(IoBuffer.wrap(packet));
    }

    @Override
    public void dispose(IoSession ioSession) throws Exception {

    }
}