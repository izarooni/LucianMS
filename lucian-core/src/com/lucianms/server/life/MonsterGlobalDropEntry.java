/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.lucianms.server.life;

import com.lucianms.server.maps.MapleMap;
import tools.Randomizer;

/**
 * @author LightPepsi
 */
public class MonsterGlobalDropEntry extends MonsterDropEntry {

    public final byte dropType;

    public MonsterGlobalDropEntry(int itemId, int chance, int continent, byte dropType, int Minimum, int Maximum, short questid) {
        super(itemId, chance, Minimum, Maximum, questid);
        this.dropType = dropType;
    }

    @Override
    public boolean shouldDrop(float mod) {
        return Randomizer.nextInt(MapleMap.MAX_DROP_CHANCE) <= chance * mod;
    }
}
