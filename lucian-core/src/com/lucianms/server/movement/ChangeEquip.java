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
package com.lucianms.server.movement;

import com.lucianms.nio.send.MaplePacketWriter;

import java.awt.*;

public class ChangeEquip implements LifeMovementFragment {

    private int update;

    public ChangeEquip(int update) {
        this.update = update;
    }

    @Override
    public void serialize(MaplePacketWriter w) {
        w.write(10);
        w.write(update);
    }

    @Override
    public Point getPosition() {
        return new Point(0, 0);
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new ChangeEquip(update);
    }
}
