const Lobby = Java.type('com.lucianms.features.carnival.MCarnivalLobbyManager');

let CarnivalLobby;

function init() {
    CarnivalLobby = new Lobby(em.getChannel());
    em.getProperties().put("lobby", CarnivalLobby);
}