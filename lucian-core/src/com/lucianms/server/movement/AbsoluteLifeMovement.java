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

import tools.data.output.LittleEndianWriter;

import java.awt.*;

public class AbsoluteLifeMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond;
    private int foothold;

    public AbsoluteLifeMovement(int type, Point position, int duration, int stance, Point pixelsPerSecond, int foothold) {
        super(type, position, duration, stance);
        this.pixelsPerSecond = pixelsPerSecond;
        this.foothold = foothold;
    }

    @Override
    public LifeMovementFragment duplicate() {
        return new AbsoluteLifeMovement(getType(),
                getPosition().getLocation(),
                getDuration(),
                getStance(),
                getPixelsPerSecond().getLocation(),
                getFoothold());
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(getType());
        lew.writePos(getPosition());
        lew.writePos(getPixelsPerSecond());
        lew.writeShort(getFoothold());
        lew.write(getStance());
        lew.writeShort(getDuration());
    }

    public Point getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(Point pixelsPerSecond) {
        this.pixelsPerSecond = pixelsPerSecond;
    }

    public int getFoothold() {
        return foothold;
    }

    public void setFoothold(int foothold) {
        this.foothold = foothold;
    }
}
