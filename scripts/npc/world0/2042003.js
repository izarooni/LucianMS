const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const Lobby = client.getChannelServer().getCarnivalLobbyManager();
const LobbyState = Java.type("com.lucianms.server.pqs.carnival.MCarnivalLobby.State");
const MCarnival = Java.type("com.lucianms.server.pqs.carnival.MCarnivalLobby");
/* izarooni */
let status = 0;
const M_Office = 980000000;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (this.carnival == null) {
        this.carnival = player.getGenericEvents().stream()
        .filter((g) => (g instanceof MCarnival))
        .findFirst().orElse(null);
    }
    if (status == 1) {
        cm.sendNext("If you have changed your mind about the battle, you may leave now");
    } else if (status == 2) {
        if (this.carnival != null) {
            // awaiting lobby
            let game = Lobby.getLobby(player.getMapId());
            game.removeParty(cm.getParty());

            game.getWaitingTask().cancel();
            game.setWaitingTask(null);

            cm.warp(M_Office);

            game.setState(LobbyState.Available);
        } else {
            player.changeMap(M_Office);
        }
        cm.dispose();
    }
}
