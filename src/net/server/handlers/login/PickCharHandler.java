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
package net.server.handlers.login;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class PickCharHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        int playerId = slea.readInt();
        int world = slea.readInt();//Wuuu? ):
        client.setWorld(world);
        String macs = slea.readMapleAsciiString();
        client.updateMacs(macs);
        if (client.hasBannedMac() || !client.playerBelongs(playerId)) {
            client.getSession().close(true);
            return;
        }
        try {
            client.setChannel(Randomizer.nextInt(Server.getInstance().getWorld(world).getChannels().size()));
        } catch (Exception e) {
            client.setChannel(1);
        }
        if (client.getIdleTask() != null) {
            client.getIdleTask().cancel(true);
        }
        try {
            client.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            String[] socket = Server.getInstance().getIP(client.getWorld(), client.getChannel()).split(":");
            client.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerId));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
