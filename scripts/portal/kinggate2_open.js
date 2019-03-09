/* Lerk
Stage 5: Door before Ergoth - Guild Quest
*/

function enter(pi) {
    let reactor = pi.getPlayer().getMap().getReactorByName("kinggate");
    if (reactor.getState() == 1) {
        pi.warp(nFieldErgoth, 2);
        if (eim.vars.ergothStart != undefined) pi.changeMusic("Bgm10/Eregos");
        return true;
    }
    else {
        pi.playerMessage(5, "This crack appears to be blocked off by the door nearby.");
        return false;
    }
}
