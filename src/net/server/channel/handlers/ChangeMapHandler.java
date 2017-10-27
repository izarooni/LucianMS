package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import net.PacketHandler;
import server.MapleInventoryManipulator;
import server.MaplePortal;
import server.MapleTrade;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ChangeMapHandler extends PacketHandler {

    private boolean eCashShop = false;
    private boolean wheelOfDestiny = false;

    private String startwp;

    private int targetMapId;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        eCashShop = slea.available() == 0; // exit Cash shop
        if (!eCashShop) {
            slea.skip(1);
            targetMapId = slea.readInt();
            startwp = slea.readMapleAsciiString();
            slea.skip(1);
            wheelOfDestiny = slea.readShort() > 0;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(player);
        }

        if (eCashShop) {
            if (!player.getCashShop().isOpened()) {
                getClient().disconnect(false, false);
                return null;
            }
            String[] socket = getClient().getChannelServer().getIP().split(":");
            player.getCashShop().open(false);
            getClient().getChannelServer().removePlayer(player);
            getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            if (player.getFakePlayer() != null) {
                player.getFakePlayer().setFollowing(true);
                player.getMap().addFakePlayer(player.getFakePlayer());
            }
            try {
                getClient().announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            if (player.getCashShop().isOpened()) {
                getClient().disconnect(false, false);
                return null;
            }
            try {
                MaplePortal portal = player.getMap().getPortal(startwp);
                if (targetMapId != -1 && !player.isAlive()) {
                    boolean executeStandardPath = true;
                    if (player.getEventInstance() != null) {
                        executeStandardPath = player.getEventInstance().revivePlayer(player);
                    }
                    if (executeStandardPath) {
                        MapleMap to = player.getMap();
                        if (wheelOfDestiny && player.getItemQuantity(5510000, false) > 0) {
                            MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.CASH, 5510000, 1, true, false);
                            player.announce(MaplePacketCreator.showWheelsLeft(player.getItemQuantity(5510000, false)));
                        } else {
                            player.cancelAllBuffs(false);
                            to = player.getMap().getReturnMap();
                            player.setStance(0);
                        }
                        player.setHp(50);
                        player.changeMap(to, to.getPortal(0));
                    }
                } else if (targetMapId != -1 && player.isGM()) {
                    MapleMap to = getClient().getChannelServer().getMapFactory().getMap(targetMapId);
                    if (to != null) {
                        player.changeMap(to, to.getPortal(0));
                    } else {
                        player.dropMessage("That map doesn't exist!");
                        player.announce(MaplePacketCreator.enableActions());
                    }
                } else if (targetMapId != -1 && !player.isGM()) {//Thanks celino for saving me some time (:
                    final int divi = player.getMapId() / 100;
                    boolean warp = false;
                    if (divi == 0) {
                        if (targetMapId == 10000) {
                            warp = true;
                        }
                    } else if (divi == 20100) {
                        if (targetMapId == 104000000) {
                            getClient().announce(MaplePacketCreator.lockUI(false));
                            getClient().announce(MaplePacketCreator.disableUI(false));
                            warp = true;
                        }
                    } else if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                        if (targetMapId == 130000000 || targetMapId / 100 == 9130401) { // Cygnus introduction
                            warp = true;
                        }
                    } else if (divi == 9140900) { // Aran Introduction
                        if (targetMapId == 914090011 || targetMapId == 914090012 || targetMapId == 914090013 || targetMapId == 140090000) {
                            warp = true;
                        }
                    } else if (divi / 10 == 1020) { // Adventurer movie clip Intro
                        if (targetMapId == 1020000) {
                            warp = true;
                        }
                    } else if (divi / 10 >= 980040 && divi / 10 <= 980045) {
                        if (targetMapId == 980040000) {
                            warp = true;
                        }
                    }
                    if (warp) {
                        final MapleMap to = getClient().getChannelServer().getMapFactory().getMap(targetMapId);
                        player.changeMap(to, to.getPortal(0));
                    }
                }
                if (portal != null && !portal.getPortalStatus()) {
                    getClient().announce(MaplePacketCreator.blockedMessage(1));
                    getClient().announce(MaplePacketCreator.enableActions());
                    return null;
                }
                if (player.getMapId() == 109040004) {
                    player.getFitness().resetTimes();
                }
                if (player.getMapId() == 109030003 || player.getMapId() == 109030103) {
                    player.getOla().resetTimes();
                }
                if (portal != null) {
                    if (portal.getPosition().distanceSq(player.getPosition()) > 400000) {
                        getClient().announce(MaplePacketCreator.enableActions());
                        return null;
                    }

                    portal.enterPortal(getClient());
                } else {
                    getClient().announce(MaplePacketCreator.enableActions());
                }
                player.setRates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean iseCashShop() {
        return eCashShop;
    }

    public boolean isWheelOfDestiny() {
        return wheelOfDestiny;
    }

    public String getStartwp() {
        return startwp;
    }

    public int getTargetMapId() {
        return targetMapId;
    }
}
