const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const LobbyState = Java.type("com.lucianms.features.carnival.MCarnivalLobby.State");
/* izarooni */
const M_Office = 980000000;
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
    if (Lobby == null) {
        cm.sendOk("The Monster Carnival is unavailable at this time.");
        return;
    }
    if (player.getMapId() != M_Office) {
        Entrance(selection);
    } else {
        Queue(selection);
    }
}

function Entrance(selection) {
    if (status == 1) {
        cm.sendSimple("#e<Competition: Monster Carnival>#n\r\nIf you're itching for some action, then the Monster Carnival is the place for you!\r\n#b"
        + "\r\n#L0#I want to participate in the Monster Carnival.#l"
        + "\r\n#L1#Tell me more about the Monster Carnival.#l"
        +" \r\n#L2#I want to trade in my Maple Shiny Coins.#l");
    } else if (status == 2) {
        if (selection == 0) {
            cm.warp(980000000);
        } else if (selection == 1) {
            cm.sendNext("The #bMonster Carnival#k is that magical place where you team up with others to obliterate hordes of monsters faster than the other folks.");
        } else if (selection == 2) {

        }
        cm.dispose();
    }
}

function Queue(selection) {
    if (status == 1) {
        if (cm.getParty() != null) {
            if (cm.isLeader()) {
                let text = "If you've gathered your party members, you may select which battle field you'd like to use\r\n#b";
                Lobby.getLobbies().entrySet().forEach(function(l) {
                    text += "\r\n#L" + l.getKey() + "##m" + l.getKey() + "# - " + l.getValue().getState() + "#l";
                });
                cm.sendSimple(text);
            } else {
                cm.sendOk("If you'd like to enter here, the leader of your party will have to talk to me. Talk to your party leader about this.");
                cm.dispose();
            }
        } else {
            cm.sendOk("You will definitely need a party to challenge others in these battle fields. Find a party to participate, or create your own.");
            cm.dispose();
        }
    } else if (status == 2) {
        let lobby = Lobby.getLobby(selection);
        if (player.isDebug() && lobby != null && lobby.getGame() != null) {
            lobby.getGame().dispose();
            cm.sendOk("The game has been disposed.");
            cm.dispose();
            return;
        }
        let userCount = cm.getPartyMembers().stream()
            .filter(m => m.getMapId() == M_Office)
            .mapToInt(m => 1).sum();
        if (lobby != null && userCount <= lobby.getMaxPartySize()) {
            if (lobby.getState() == LobbyState.Waiting || lobby.getState() == LobbyState.Available) {
                if (lobby.canEnter(cm.getParty())) {
                    // warp present party members to the lobby
                    if (lobby.joiningParty(cm.getParty())) {
                        // challenger found
                        lobby.setState(LobbyState.Starting);
                    } else {
                        // available lobby
                        lobby.setState(LobbyState.Waiting);
                    }
                } else {
                    cm.sendOk("Your party may be too large or small to battle this party");
                }
            } else {
                cm.sendOk("There is a battle on-going in this area.");
            }
        } else {
            cm.sendOk("You must have #b" + game.getMaxPartySize() + " party members#k to challenge in this battle field.");
        }
        cm.dispose();
    }
}
