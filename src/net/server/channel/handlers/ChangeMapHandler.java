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
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MaplePortal;
import server.MapleTrade;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ChangeMapHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();

        if (player.isBanned()) {
            return;
        }
        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(player);
        }
        if (slea.available() == 0) { //Cash Shop :)
            if (!player.getCashShop().isOpened()) {
                c.disconnect(false, false);
                return;
            }
            String[] socket = c.getChannelServer().getIP().split(":");
            player.getCashShop().open(false);
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            try {
                c.announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            if (player.getCashShop().isOpened()) {
                c.disconnect(false, false);
                return;
            }
            try {
                slea.readByte(); // 1 = from dying 0 = regular portals
                int targetid = slea.readInt();
                String startwp = slea.readMapleAsciiString();
                MaplePortal portal = player.getMap().getPortal(startwp);
                slea.readByte();
                boolean wheel = slea.readShort() > 0;
                if (targetid != -1 && !player.isAlive()) {
                    boolean executeStandardPath = true;
                    if (player.getEventInstance() != null) {
                        executeStandardPath = player.getEventInstance().revivePlayer(player);
                    }
                    if (executeStandardPath) {
                        MapleMap to = player.getMap();
                        if (wheel && player.getItemQuantity(5510000, false) > 0) {
                            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                            player.announce(MaplePacketCreator.showWheelsLeft(player.getItemQuantity(5510000, false)));
                        } else {
                            player.cancelAllBuffs(false);
                            to = player.getMap().getReturnMap();
                            player.setStance(0);
                        }
                        player.setHp(50);
                        player.changeMap(to, to.getPortal(0));
                    }
                } else if (targetid != -1 && player.isGM()) {
                    MapleMap to = c.getChannelServer().getMapFactory().getMap(targetid);
                    if (to != null) {
                        player.changeMap(to, to.getPortal(0));
                    } else {
                        player.dropMessage("That map doesn't exist!");
                        player.announce(MaplePacketCreator.enableActions());
                    }
                } else if (targetid != -1 && !player.isGM()) {//Thanks celino for saving me some time (:
                    final int divi = player.getMapId() / 100;
                    boolean warp = false;
                    if (divi == 0) {
                        if (targetid == 10000) {
                            warp = true;
                        }
                    } else if (divi == 20100) {
                        if (targetid == 104000000) {
                            c.announce(MaplePacketCreator.lockUI(false));
                            c.announce(MaplePacketCreator.disableUI(false));
                            warp = true;
                        }
                    } else if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                        if (targetid == 130000000 || targetid / 100 == 9130401) { // Cygnus introduction
                            warp = true;
                        }
                    } else if (divi == 9140900) { // Aran Introduction
                        if (targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000) {
                            warp = true;
                        }
                    } else if (divi / 10 == 1020) { // Adventurer movie clip Intro
                        if (targetid == 1020000) {
                            warp = true;
                        }
                    } else if (divi / 10 >= 980040 && divi / 10 <= 980045) {
                        if (targetid == 980040000) {
                            warp = true;
                        }
                    }
                    if (warp) {
                        final MapleMap to = c.getChannelServer().getMapFactory().getMap(targetid);
                        player.changeMap(to, to.getPortal(0));
                    }
                }
                if (portal != null && !portal.getPortalStatus()) {
                    c.announce(MaplePacketCreator.blockedMessage(1));
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                if (player.getMapId() == 109040004) {
                    player.getFitness().resetTimes();
                }
                if (player.getMapId() == 109030003 || player.getMapId() == 109030103) {
                    player.getOla().resetTimes();
                }
                if (portal != null) {
                    if (portal.getPosition().distanceSq(player.getPosition()) > 400000) {
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    }

                    portal.enterPortal(c);
                } else {
                    c.announce(MaplePacketCreator.enableActions());
                }
                player.setRates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
