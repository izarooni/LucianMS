package net.discord;

import net.discord.handlers.DiscordRequest;
import net.discord.handlers.DiscordRequestManager;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericLittleEndianAccessor;

/**
 * Not much needs to be done here.
 * <p>
 * Receive packet > handle > respond
 * </p>
 *
 * @author izarooni
 */
public class DiscordHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordHandler.class);


    public DiscordHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        LOGGER.info("Session #{} created", session.getId());
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        LOGGER.info("Session #{} opened", session.getId());

        DiscordListener.setSession(session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        LOGGER.info("Session #{} closed", session.getId());
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        LOGGER.info("Session #{} idle status {}", session.getId(), status.toString());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LOGGER.info("Session #{} exception", session.getId(), cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        LOGGER.info("Session #{} received message {}", session.getId(), message.getClass().getSimpleName());
        if (message instanceof byte[]) {
            byte[] bytes = (byte[]) message;
            GenericLittleEndianAccessor lea = new GenericLittleEndianAccessor(new ByteArrayByteStream(bytes));
            byte header = lea.readByte();
            DiscordRequest request = DiscordRequestManager.getRequest(header);
            request.handle(lea);
        } else {
            LOGGER.info("Unhandled message type {}\r\n{}", message.getClass(), message);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        LOGGER.info("Session #{} sent message", session.getId());
    }
}
