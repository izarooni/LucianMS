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
/*
 * @author BubblesDev, Rich for text
 */
importPackage(Packages.tools);
var LifeFactory = Java.type("server.life.MapleLifeFactory");
function start() {
    cm.sendOk("it's been long since I've spotted Aeonaxx around here. Keep an eye out, will you? \r\n\\r\n\#eWho is #rAeonaxx#k?\r\n\Aeonaxx is a dragon that sometimes makes his presence here. He is a creation from the Black Mage but he's harmless unless you aggro him. If you dare to take him on then go ahead. i won't protect you.");
    cm.dispose();
	cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear3"));
}