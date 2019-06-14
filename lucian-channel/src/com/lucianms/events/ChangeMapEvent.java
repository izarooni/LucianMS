package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ServerConstants;
import com.lucianms.features.GenericEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MaplePortal;
import com.lucianms.server.MapleTrade;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

public class ChangeMapEvent extends PacketEvent {

    private boolean fake = false;
    private boolean eCashShop = false;
    private boolean wheelOfDestiny = false;

    private String startwp;

    private int targetMapId;

    /**
     * For packet event reflective instantiation
     */
    public ChangeMapEvent() {
    }

    /**
     * Used for handling of {@link com.lucianms.features.GenericEvent}
     */
    public ChangeMapEvent(int targetMapId) {
        this.targetMapId = targetMapId;

        fake = true;
        this.eCashShop = false;
        this.wheelOfDestiny = false;
        this.startwp = null;
        this.targetMapId = -1;
    }

    @Override
    public void exceptionCaught(MaplePacketReader reader, Throwable t) {
        getClient().announce(MaplePacketCreator.enableActions());
        super.exceptionCaught(reader, t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        eCashShop = reader.available() == 0; // exit Cash shop
        if (!eCashShop) {
            reader.skip(1);
            targetMapId = reader.readInt();
            startwp = reader.readMapleAsciiString();
            reader.skip(1);
            wheelOfDestiny = reader.readShort() > 0;
        }
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleWorld world = client.getWorldServer();
        MapleChannel ch = client.getChannelServer();
        MapleCharacter player = client.getPlayer();
        MaplePortal portal = player.getMap().getPortal(startwp);

        if (portal != null && player.isGM() && player.isDebug()) {
            player.sendMessage("[DEBUG] ID: {}, Name: {}, Script: {}, Target map: {}:{}, Location: x:{}/y:{}", portal.getId(), portal.getName(), portal.getScriptName(), portal.getTarget(), portal.getTargetMapId(), portal.getPosition().x, portal.getPosition().y);
            player.announce(MaplePacketCreator.enableActions());
            return null;
        }

        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(player);
        }

        if (eCashShop) {
            if (!player.getCashShop().isOpened()) {
                client.disconnect();
                return null;
            }
            player.getCashShop().open(false);
            world.getPlayerStorage().remove(player.getId());
            client.updateLoginState(LoginState.Transfer);
            if (player.getFakePlayer() != null) {
                player.getFakePlayer().setFollowing(true);
                player.getMap().addFakePlayer(player.getFakePlayer());
            }
            player.saveToDB();
            client.announce(MaplePacketCreator.getChannelChange(ch.getNetworkAddress(), ch.getPort()));
        } else {
            if (player.getCashShop().isOpened()) {
                client.disconnect();
                return null;
            }
            try {
                if (targetMapId != -1 && !player.isAlive()) {
                    boolean executeStandardPath = true;
                    if (player.getEventInstance() != null) {
                        executeStandardPath = player.getEventInstance().revivePlayer(player);
                    }
                    if (executeStandardPath) {
                        MapleMap to = player.getMap();
                        if (wheelOfDestiny && player.getItemQuantity(5510000, false) > 0) {
                            MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5510000, 1, true, false);
                            player.announce(MaplePacketCreator.showWheelsLeft(player.getItemQuantity(5510000, false)));
                        } else {
                            player.cancelAllBuffs();
                            for (GenericEvent event : player.getGenericEvents()) {
                                if (event.onPlayerDeath(this, player)) {
                                    return null;
                                }
                            }
                            to = player.getMap().getReturnMap();
                            if (to == null) {
                                getLogger().warn("Player '{}' unable to return to map {}", player.getName(), player.getMap().getReturnMapId());
                                player.sendMessage("The return map is obstructed");
                                to = ch.getMap(ServerConstants.HOME_MAP);
                            }
                            player.setStance(0);
                        }
                        player.setHp(50);
                        player.changeMap(to, to.getPortal(0));
                    }
                } else if (targetMapId != -1 && player.isGM()) {
                    MapleMap to = ch.getMap(targetMapId);
                    if (to != null) {
                        player.changeMap(to);
                    }
                } else if (targetMapId != -1 && !player.isGM()) { // Thanks celino for saving me some time (:
                    final int divi = player.getMapId() / 100;
                    boolean warp = false;
                    if (divi == 0) {
                        if (targetMapId == 10000) {
                            warp = true;
                        }
                    } else if (divi == 20100) {
                        if (targetMapId == 104000000) {
                            client.announce(MaplePacketCreator.lockUI(false));
                            client.announce(MaplePacketCreator.disableUI(false));
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
                        final MapleMap to = ch.getMap(targetMapId);
                        player.changeMap(to, to.getPortal(0));
                    }
                }
                if (portal != null && !portal.getPortalStatus()) {
                    client.announce(MaplePacketCreator.blockedMessage(1));
                    client.announce(MaplePacketCreator.enableActions());
                    return null;
                }
                if (player.getMapId() == 109040004) {
                    player.getFitness().resetTimes();
                }
                if (player.getMapId() == 109030003 || player.getMapId() == 109030103) {
                    player.getOla().resetTimes();
                }
                if (portal == null || portal.getPosition().distanceSq(player.getPosition()) > 400000 || !portal.enterPortal(client)) {
                    getLogger().info("portal '{}' in '{}' that leads to invalid area: '{}'",
                            startwp, player.getMapId(), portal);
                    client.announce(MaplePacketCreator.enableActions());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isFake() {
        return fake;
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
