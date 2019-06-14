const LobbyState = Java.type("com.lucianms.features.carnival.MCarnivalLobby.State");
/* izarooni */
let status = 0;
let Lobby = null;
{
    let em = cm.getEventManager("MonsterCarnival");
    if (em != null) {
        Lobby = em.getProperties().get("lobby");
    }
}

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
        // waiting lobby
        let party = cm.getParty();
        let lobby = Lobby.getLobby(player.getMapId());
        lobby.setState(lobby.removeParty(party) ? LobbyState.Available : LobbyState.Waiting);
        cm.dispose();
    }
}
