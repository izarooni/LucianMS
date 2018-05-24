var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var Lobby = client.getChannelServer().getCarnivalLobbyManager();
var LobbyState = Java.type("com.lucianms.server.pqs.carnival.MCarnivalLobby.State");
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
                var text = "If you've gathered your party members, you may select which battle field you'd like to use\r\n#b";
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
        var game = Lobby.getLobby(selection);
        if (player.isDebug()) {
            game.getGame().dispose();
            cm.sendOk("The game has been disposed.");
            cm.dispose();
        }
        var userCount = 0;
        cm.getPartyMembers().forEach(function(chr) {
            if (chr.getMapId() == M_Office) {
                userCount++;
            }
        });
        if (game != null && userCount <= game.getMaxPartySize()) {
            if (game.getState() == LobbyState.Waiting || game.getState() == LobbyState.Available) {
                if (game.canEnter(cm.getParty())) {
                    if (game.joiningParty(cm.getParty())) {
                        // challenger found
                        game.setState(LobbyState.Starting);
                    } else {
                        // available lobby
                        game.setState(LobbyState.Waiting);
                    }
                    // warp present party members to the lobby
                    cm.getPartyMembers().forEach(function(chr) {
                        if (chr.getMapId() == M_Office) {
                            // warp to lobby
                            chr.changeMap(game.getBattlefieldMapId() - 1); // battle field lobby
                        }
                        // if lobby is waiting, add clock for timeout
                        if (game.getState() == LobbyState.Waiting) {
                            chr.announce(MaplePacketCreator.getClock(300));
                        } else if (game.getState() == LobbyState.Starting) {
                            // challenging party
                            chr.announce(MaplePacketCreator.getClock(10));
                        }
                    });
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
