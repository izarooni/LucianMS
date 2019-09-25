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
package com.lucianms.client;

public enum MapleDisease {

    //@formatter:off
    NULL        (null,                     0x0, false),
    SLOW        (MapleBuffStat.SLOW,            0x1, false),
    SEDUCE      (MapleBuffStat.ATTRACT,         0x80, false),
    FISHABLE    (null,                    0x100, false),
    CONFUSE     (MapleBuffStat.REVERSE_INPUT,   0x80000, false),
    STUN        (MapleBuffStat.STUN,            0x2000000000000L, false),
    POISON      (MapleBuffStat.POISON,          0x4000000000000L, true),
    SEAL        (MapleBuffStat.SEAL,            0x8000000000000L, false),
    DARKNESS    (MapleBuffStat.DARKNESS,        0x10000000000000L, false),
    WEAKEN      (MapleBuffStat.WEAKNESS,        0x4000000000000000L, false),
    CURSE       (MapleBuffStat.CURSE,           0x8000000000000000L, false);
    //@formatter:on
    private final MapleBuffStat buff;
    private final long bitmask;
    private final boolean first;

    MapleDisease(MapleBuffStat buff, long bitmask, boolean first) {
        this.buff = buff;
        this.bitmask = bitmask;
        this.first = first;
    }

    public static MapleDisease valueOf(int skillID) {
        switch (skillID) {
            default:
                throw new IllegalArgumentException(String.valueOf(skillID));
            case 120:
                return SEAL;
            case 121:
                return DARKNESS;
            case 122:
                return WEAKEN;
            case 123:
                return STUN;
            case 124:
                return CURSE;
            case 125:
                return POISON;
            case 126:
                return SLOW;
            case 128:
                return SEDUCE;
            case 132:
                return CONFUSE;
        }
    }

    public MapleBuffStat getBuff() {
        return buff;
    }

    public long getValue() {
        return bitmask;
    }

    public boolean isFirst() {
        return first;
    }
}
