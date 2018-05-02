package com.lucianms.discord;

import com.lucianms.discord.handlers.DiscordRequest;
import com.lucianms.discord.handlers.DiscordRequestManager;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericLittleEndianAccessor;

import java.io.IOException;

/**
 * Not much needs to be done here.
 * <p>
 * Receive packet > handle > respond
 * </p>
 *
 * @author izarooni
 */
public class DiscordSessionHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSessionHandler.class);


    DiscordSessionHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) {
        LOGGER.info("Session #{} created", session.getId());
    }

    @Override
    public void sessionOpened(IoSession session) {
        LOGGER.info("Session #{} opened", session.getId());

        DiscordSession.setSession(session);
    }

    @Override
    public void sessionClosed(IoSession session) {
        LOGGER.info("Session #{} closed", session.getId());
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        if (cause instanceof IOException) {
            return;
        }
        LOGGER.info("Session #{} exception", session.getId(), cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if (message instanceof byte[]) {
            byte[] bytes = (byte[]) message;
            GenericLittleEndianAccessor lea = new GenericLittleEndianAccessor(new ByteArrayByteStream(bytes));
            byte header = lea.readByte();
            DiscordRequest request = DiscordRequestManager.getRequest(header);
            if (request != null) {
                try {
                    LOGGER.info("{} handler requested", request.getClass().getSimpleName());
                    request.handle(lea);
                } catch (Throwable t) {
                    LOGGER.error("Failed to handle packet 0x{}", Integer.toHexString(header));
                    t.printStackTrace();
                }
            } else {
                LOGGER.info("Packet header not handler 0x{}", Integer.toHexString(header));
            }
        } else {
            LOGGER.info("Unhandled message type {}\r\n{}", message.getClass(), message);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) {
        LOGGER.info("Session #{} sent message", session.getId());
    }
}
