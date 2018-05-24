var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var Lobby = client.getChannelServer().getCarnivalLobbyManager();
var LobbyState = Java.type("com.lucianms.server.pqs.carnival.MCarnivalLobby.State");
var MCarnival = Java.type("com.lucianms.server.pqs.carnival.MCarnivalLobby");
/* izarooni */
var status = 0;
var M_Office = 980000000;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (this.carnival == null) {
        this.carnival = player.getGenericEvents().stream().filter(function(g) { return (g instanceof MCarnival)} ).findFirst();
    }
    if (status == 1) {
        cm.sendNext("If you have changed your mind about the battle, you may leave now");
    } else if (status == 2) {
        if (this.carnival.isPresent()) {
            // only when 2 parties are present
            this.carnival = this.carnival.get();
        } else {
            // awaiting lobby
            var game = Lobby.getLobby(player.getMapId());
            game.removeParty(cm.getParty());

            game.getWaitingTask().cancel();
            game.setWaitingTask(null);

            cm.warp(M_Office);

            game.setState(LobbyState.Available);
            cm.dispose();
        }
    }
}
