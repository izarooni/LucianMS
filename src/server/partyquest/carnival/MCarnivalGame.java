package server.partyquest.carnival;

import client.MapleCharacter;
import client.MapleStat;
import net.server.channel.handlers.ChangeMapHandler;
import server.events.custom.GenericEvent;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

/**
 * @author izarooni
 */
public class MCarnivalGame extends GenericEvent {

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
        player.announce(MaplePacketCreator.showForcedEquip(player.getTeam()));
        player.announce(MaplePacketCreator.getMonsterCarnivalStart(player, this));
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(980000000); // hub
        player.announce(MaplePacketCreator.getMonsterCarnivalStop(player));
    }

    @Override
    public void onPlayerDeath(MapleCharacter player) {
    }

    @PacketWorker
    public void onFieldChange(ChangeMapHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        if (event.getTargetMapId() != -1 && !player.isAlive()) {
            player.changeMap(lobby.getBattlefieldMapId() + 1);
            player.setHp(50);
            player.updateSingleStat(MapleStat.HP, 50);
        }
        event.setCanceled(true);
    }

    public long getTimeLeft() {
        return 60000 - (System.currentTimeMillis() - startTimestamp);
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
