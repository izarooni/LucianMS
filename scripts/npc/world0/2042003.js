const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const Lobby = client.getChannelServer().getCarnivalLobbyManager();
const LobbyState = Java.type("com.lucianms.features.carnival.MCarnivalLobby.State");
const MCarnival = Java.type("com.lucianms.features.carnival.MCarnivalLobby");
/* izarooni */
const M_Office = 980000000;
let status = 0;
let carnival = player.getGenericEvents().stream().filter((g) => (g instanceof MCarnival)).findFirst().orElse(null);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("If you have changed your mind about the battle, you may leave now");
    } else if (status == 2) {
        if (carnival != null) {
            // awaiting lobby
            let lobby = Lobby.getLobby(player.getMapId());
            lobby.setState(lobby.removeParty(cm.getParty()) ? LobbyState.Available : LobbyState.Waiting);

            cm.warp(M_Office);
        } else {
            player.changeMap(M_Office);
        }
        cm.dispose();
    }
}
