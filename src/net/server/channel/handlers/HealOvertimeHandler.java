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
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class HealOvertimeHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.HealOvertime);

        int timestamp = slea.readInt();
        slea.skip(4);
        short healHP = slea.readShort();
        short healMP = slea.readShort();

        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 200) {
            entry.spamCount++;
            return;
        } else {
            entry.spamCount = 0;
        }
        entry.latestOperationTimestamp = System.currentTimeMillis();

        if (healHP != 0) {
            int abHeal = 140;
            if (player.getMapId() == 105040401 || player.getMapId() == 105040402 || player.getMapId() == 809000101 || player.getMapId() == 809000201) {
                abHeal += 40; // Sleepywood sauna and showa spa...
            }
            if (healHP > abHeal) {
                entry.cheatCount++;
                entry.latestCheatTimestamp = System.currentTimeMillis();
                entry.announce(c, String.format("%s now has %d cheat points for fast healing", player.getName(), entry.cheatCount), 5000);
            }
            player.addHP(healHP);
            player.checkBerserk();
        }

        if (healMP > 0 && healMP < 1000) {
            player.addMP(healMP);
        }
    }
}
