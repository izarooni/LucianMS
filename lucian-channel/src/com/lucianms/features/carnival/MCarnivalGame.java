package com.lucianms.features.carnival;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.events.ChangeMapEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MaplePartyCharacter;

import java.util.Collection;

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
        if (player.addGenericEvent(this)) {
            if (startTimestamp == -1) {
                startTimestamp = System.currentTimeMillis();
            }
            player.changeMap(lobby.getBattlefieldMapId());
        }
    }

    private void removePlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(M_Office);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        removePlayer(player);

        MCarnivalTeam team = getTeam(player.getTeam());
        MaplePartyCharacter leader = team.getParty().getLeader();
        boolean isLeader = (leader.getPlayerID() == player.getId());
        MapleCharacter newLeader = null;
        if (isLeader) {
            Collection<MapleCharacter> players = team.getParty().getPlayers(p -> p.getId() != player.getId());
            newLeader = players.stream().findAny().orElse(null);
            players.clear();
            if (newLeader == null) {
                dispose();
                lobby.setState(MCarnivalLobby.State.Available);
                return;
            }
        }

        player.announce(MCarnivalPacket.getMonsterCarnivalStop((byte) team.getId(), isLeader, (isLeader) ? newLeader.getName() : player.getName()));
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        player.setMapId(M_Office);
        unregisterPlayer(player);
    }

    @Override
    public boolean onPlayerDeath(Object sender, MapleCharacter player) {
        if (sender == null) {
            MapleMap map = player.getClient().getChannelServer().getMap(lobby.getBattlefieldMapId());
            map.broadcastMessage(MCarnivalPacket.getMonsterCarnivalPlayerDeath(player));
        }
        return false;
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

    public void broadcastMessage(MCarnivalTeam team, String content, Object... args) {
        if (team == null || team.getId() == 0) {
            teamRed.getParty().sendMessage(5, content, args);
        }
        if (team == null || team.getId() == 1) {
            teamBlue.getParty().sendMessage(5, content, args);
        }
    }

    public void dispose() {
        Collection<MapleCharacter> players;
        if (teamRed != null && teamRed.getParty() != null) {
            players = teamRed.getParty().getPlayers();
            players.forEach(this::removePlayer);
            players.clear();
        }
        teamRed = null;
        if (teamBlue != null && teamBlue.getParty() != null) {
            players = teamBlue.getParty().getPlayers();
            players.forEach(this::removePlayer);
            players.clear();
        }
        teamBlue = null;
        MapleMap map = lobby.getChannel().removeMap(lobby.getBattlefieldMapId());
        if (map != null) {
            map.killAllMonsters();
            map.clearDrops();
        }
    }

    public long getTimeLeft() {
        return (((1000 * 60 * 10) + startTimestamp) - System.currentTimeMillis());
    }

    public MCarnivalLobby getLobby() {
        return lobby;
    }

    public MCarnivalTeam getTeam(int team) {
        if (team != 0 && team != 1) {
            throw new IllegalArgumentException("Team can only be 0 or 1");
        }
        return team == 0 ? teamRed : teamBlue;
    }

    public MCarnivalTeam getTeamOpposite(int team) {
        if (team != 0 && team != 1) {
            throw new IllegalArgumentException("Team can only be 0 or 1");
        }
        return team == 0 ? teamBlue : teamRed;
    }

    public MCarnivalTeam getTeamRed() {
        return teamRed;
    }

    public void setTeamRed(MCarnivalTeam teamRed) {
        this.teamRed = teamRed;

        Collection<MapleCharacter> players = teamRed.getParty().getPlayers();
        players.forEach(p -> p.setTeam(0));
        players.clear();
    }

    public MCarnivalTeam getTeamBlue() {
        return teamBlue;
    }

    public void setTeamBlue(MCarnivalTeam teamBlue) {
        this.teamBlue = teamBlue;

        Collection<MapleCharacter> players = teamBlue.getParty().getPlayers();
        players.forEach(p -> p.setTeam(1));
        players.clear();
    }
}
