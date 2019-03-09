load('scripts/util_gpq.js');
/* izarooni */
function enter(pi) {
    let eim = pi.getPlayer().getEventInstance();
    
    if (eim == null) {
        pi.getPlayer().sendMessage(5, "Unable to enter because of the force of the ground.");
        return false;
    } else if (!eim.vars.canEnter) {
        if (!pi.isLeader()) {
            pi.getPlayer().sendMessage(5, "The portal is not open yet.");
            return false;
        }
    }
    eim.vars.canEnter = true;
    pi.warp(nFieldGPQValley);
    return true;
}