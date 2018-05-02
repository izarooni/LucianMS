package com.lucianms.server.pqs.carnival;

import client.MapleCharacter;
import client.MapleStat;
import com.lucianms.server.events.channel.ChangeMapEvent;
import com.lucianms.features.GenericEvent;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import com.lucianms.lang.annotation.PacketWorker;

/**
 * @author izarooni
 */
public class MCarnivalGame extends GenericEvent {

    private static final int M_Office = 980000000;

    private final MCarnivalLobby lobby;
    private long startTimestamp = -1;
    private MCarnivalTeam teamRed = null;
    private MCarnivalTeam teamBlue = null;

    public MCarnivalGame(MCarnivalLobby lobby) {
        this.lobby = lobby;
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (startTimestamp == -1) {
            startTimestamp = System.currentTimeMillis();
        }
        player.addGenericEvent(this);
        player.changeMap(lobby.getBattlefieldMapId());
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(M_Office);
        player.announce(MaplePacketCreator.getMonsterCarnivalStop(player));
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        player.setMapId(M_Office);
    }

    @Override
    public void onPlayerDeath(MapleCharacter player) {
        MapleMap map = player.getClient().getChannelServer().getMap(lobby.getBattlefieldMapId());
        map.broadcastMessage(MaplePacketCreator.getMonsterCarnivalPlayerDeath(player));
    }

    @PacketWorker
    public void onFieldChange(ChangeMapEvent event) {
        MapleCharacter player = event.getClient().getPlayer();
        if (event.getTargetMapId() != -1 && !player.isAlive()) {
            player.changeMap(lobby.getBattlefieldMapId() + 1);
            player.setHp(50);
            player.updateSingleStat(MapleStat.HP, 50);
        }
        event.setCanceled(true);
    }

    public void dispose() {
        MapleMap map = lobby.getChannel().removeMap(lobby.getBattlefieldMapId());
        map.killAllMonsters();
        map.clearDrops();
        lobby.setState(MCarnivalLobby.State.Available);
    }

    public long getTimeLeft() {
        return (((1000 * 60 * 10) + startTimestamp) - System.currentTimeMillis());
    }

    public MCarnivalLobby getLobby() {
        return lobby;
    }

    public MCarnivalTeam getTeam(int team) {
        if (team != 0 && team != 1) {
            return null;
        }
        return team == 0 ? teamRed : teamBlue;
    }

    public MCarnivalTeam getTeamOpposite(int team) {
        if (team != 0 && team != 1) {
            return null;
        }
        return team == 0 ? teamBlue : teamRed;
    }

    public MCarnivalTeam getTeamRed() {
        return teamRed;
    }

    public void setTeamRed(MCarnivalTeam teamRed) {
        this.teamRed = teamRed;
        this.teamRed.getParty().getMembers().forEach(p -> p.getPlayer().setTeam(0));
    }

    public MCarnivalTeam getTeamBlue() {
        return teamBlue;
    }

    public void setTeamBlue(MCarnivalTeam teamBlue) {
        this.teamBlue = teamBlue;
        this.teamRed.getParty().getMembers().forEach(p -> p.getPlayer().setTeam(1));
    }
}
