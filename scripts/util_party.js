/* izarooni */

function partyExists(cm) {
    return cm.getParty() != null;
}

function partySizecm(cm) {
    return cm.getParty().getMembers().size();
}

function membersPresent(cm, m) {
    var ret = { present: [], away: [] };
    var iter = cm.getParty().getMembers().iterator();
    while (iter.hasNext()) {
        var mpc = iter.next();
        if (mpc.isOnline() && mpc.getMapId() == m) {
            ret.present.push(mpc.getId());
        } else {
            ret.away.push(mpc.getId());
        }
    }
    return ret;
}