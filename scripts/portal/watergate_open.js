load('scripts/util_gpq.js');
/* izarooni */
function enter(pi) {
    let reactor = pi.getPlayer().getMap().getReactorByName("watergate");
    if (reactor.getState() == 1) {
        pi.warp(nFieldUndergroundWaterway);
        return true;
    }
    return false;
}
