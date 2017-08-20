package net.discord;

import net.discord.proto.DiscordDecoder;
import net.discord.proto.DiscordEncoder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Use basic encoding/decoding, socket should be local ONLY
 *
 * @author izarooni
 */
public class DiscordListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordListener.class);

    private static IoAcceptor acceptor = null;

    private static IoSession session = null;

    public static synchronized void listen() {
        if (acceptor != null) {
            acceptor.dispose();
            acceptor = null;
        }
        try {
            acceptor = new NioSocketAcceptor();
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory() {
                private DiscordDecoder decoder = new DiscordDecoder();
                private DiscordEncoder encoder = new DiscordEncoder();

                @Override
                public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
                    return encoder;
                }

                @Override
                public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
                    return decoder;
                }
            }));
            acceptor.setHandler(new DiscordHandler());
            acceptor.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 8483));
            LOGGER.info("Discord now listening on port 8483");
        } catch (IOException e) {
            LOGGER.warn("Unable to create Discord listener on port 8483", e);
        }
    }

    public static synchronized void ignore() {
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose();
            LOGGER.info("Discord listener unbind from 8483");
        } else {
            LOGGER.info("No Discord listener assigned");
        }
    }

    public static IoSession getSession() {
        return session;
    }

    public static void setSession(IoSession session) {
        DiscordListener.session = session;
    }
}
