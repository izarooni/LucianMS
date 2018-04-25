/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import net.server.Server;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.FilePrinter;
import tools.MapleAESOFB;
import tools.MapleLogger;
import tools.MaplePacketCreator;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

public class MapleServerHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleServerHandler.class);

    private PacketProcessor processor;
    private int world = -1, channel = -1;

    public MapleServerHandler() {
        this.processor = PacketProcessor.getProcessor(-1, -1);
    }

    public MapleServerHandler(int world, int channel) {
        this.processor = PacketProcessor.getProcessor(world, channel);
        this.world = world;
        this.channel = channel;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (cause instanceof IOException || cause instanceof ClassCastException) {
            return;
        }
        MapleClient mc = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (mc != null && mc.getPlayer() != null) {
            FilePrinter.printError(FilePrinter.EXCEPTION_CAUGHT, cause, "Exception caught by: " + mc.getPlayer());
        }
    }

    @Override
    public void sessionOpened(IoSession session) {
        if (!Server.getInstance().isOnline()) {
            session.closeNow();
            return;
        }
        if (world > -1 && channel > -1 && Server.getInstance().getChannel(world, channel) == null) {
            session.closeNow();
            return;
        }
        String address = session.getRemoteAddress().toString().substring(1).split(":")[0];
        if (!address.equals("0")) { // web access
            LOGGER.info("Session {} created by {}", session.getId(), address);
        }

        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};
        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};
        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);

        MapleAESOFB sendCypher = new MapleAESOFB(key, ivSend, (short) (0xFFFF - ServerConstants.VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(key, ivRecv, ServerConstants.VERSION);

        MapleClient client = new MapleClient(sendCypher, recvCypher, session);
        client.setWorld(world);
        client.setChannel(channel);

        Random r = new Random();
        client.setSessionId(r.nextLong());

        session.write(MaplePacketCreator.getHello(ServerConstants.VERSION, ivSend, ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
    }

    @Override
    public void sessionClosed(IoSession session) {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            try {
                boolean inCashShop = false;
                if (client.getPlayer() != null) {
                    inCashShop = client.getPlayer().getCashShop().isOpened();
                }
                client.disconnect(false, inCashShop);
            } catch (Throwable t) {
                FilePrinter.printError(FilePrinter.ACCOUNT_STUCK, t);
            } finally {
                session.closeNow();
                session.removeAttribute(MapleClient.CLIENT_KEY);
                //client.empty();
            }
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            byte[] content = (byte[]) message;
            SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(content));
            short packetId = slea.readShort();
            {
                final MaplePacketHandler handler = processor.getHandler(packetId);
                if (handler != null) {
                    if (handler.validateState(client)) {
                        try {
                            MapleLogger.logRecv(client, packetId, message);
                            handler.handlePacket(slea, client);
                        } catch (final Exception t) {
                            LOGGER.info(slea.toString());
                            LOGGER.error("Unable to process handler {}, user {} player {}", handler.getClass().getSimpleName(), client.getAccountName(), (client.getPlayer() != null ? client.getPlayer().getName() : "N/A"));
                            t.printStackTrace();
                        }
                    }
                    return;
                }
            }
            {
                MapleCharacter player = client.getPlayer();
                // isolate the variables until removing deprecated packet handlers, even though there's no need to... dw about it
                Class<? extends PacketHandler> clazz = PacketManager.getHandler(packetId);
                if (clazz != null) {
                    try {
                        PacketHandler handler = clazz.newInstance();
                        handler.setClient(client);
                        if (handler.inValidState()) {
                            handler.process(slea);
                            try {
                                player.getGenericEvents().forEach(e -> e.onPacketEvent(handler));
                                if (!handler.isCanceled()) {
                                    handler.onPacket();
                                    handler.post();
                                }
                            } catch (Exception t) {
                                handler.exceptionCaught(t);
                            }
                        }
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) {
    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        super.sessionIdle(session, status);
        MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        if (client != null) {
            client.sendPing();
        }
    }
}
