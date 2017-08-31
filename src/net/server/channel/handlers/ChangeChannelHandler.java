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
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.Cheater;
import client.autoban.Cheats;
import net.AbstractMaplePacketHandler;
import server.events.custom.scheduled.SOuterSpace;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public class ChangeChannelHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        int channel = slea.readByte() + 1;
        if (c.getChannel() == channel) {
            return;
        }
        if (c.getPlayer().getCashShop().isOpened() || c.getPlayer().getMiniGame() != null || c.getPlayer().getMapId() == 333 || c.getPlayer().getPlayerShop() != null) {
            return;
        }
        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.FastChannelChange);
        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 1000) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if (player.getMapId() == 98) { // outer space map
            player.dropMessage(1, "You can't do it here in this map.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        entry.latestOperationTimestamp = System.currentTimeMillis();
        c.changeChannel(channel);
    }
}