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
package com.lucianms.events;

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.client.meta.Occupation;
import com.lucianms.nio.receive.MaplePacketReader;

public final class PlayerDealDamageTouchEvent extends AbstractDealDamageEvent {

    private AttackInfo attackInfo;

    public PlayerDealDamageTouchEvent() {
        onPost(new Runnable() {
            @Override
            public void run() {
                if (!isCanceled()) {
                    MapleCharacter player = getClient().getPlayer();
                    Occupation occupation = player.getOccupation();
                    if (occupation != null && occupation.getType() == Occupation.Type.Undead
                            && attackInfo != null && !attackInfo.allDamage.isEmpty()) {
                        int gain = (int) (player.getMaxHp() * Math.random() * 0.07 + 0.03);
                        player.addHP(gain);
                        player.updateSingleStat(MapleStat.HP, player.getHp());
                    }
                }
            }
        });
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        attackInfo = parseDamage(reader, false, false);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getEnergyBar() == 15000 || player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
            applyAttack(player, attackInfo, 1);
        }
        return null;
    }

    public AttackInfo getAttackInfo() {
        return attackInfo;
    }
}
