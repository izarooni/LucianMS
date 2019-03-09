load('scripts/util_gpq.js');
/* izarooni */
function enter(pi) {
    let reactor = pi.getPlayer().getMap().getReactorByName("statuegate");
    if (reactor.getState() == 1) {
        pi.warp(nFieldGPQRoad);
        return true;
    } else {
        pi.getPlayer().dropMessage(5, "The gate is closed.");
        return false;
    }
}