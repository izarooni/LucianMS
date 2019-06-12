/* izarooni */

function partyExists(cm) {
    return cm.getParty() != null;
}

function partySize(cm) {
    return cm.getParty().size();
}

function membersPresent(cm, m) {
    var ret = { present: [], away: [] };
    var iter = cm.getParty().values().iterator();
    while (iter.hasNext()) {
        var mpc = iter.next();
        if (mpc.getPlayer() != null && mpc.getFieldID() == m) {
            ret.present.push(mpc.getPlayerID());
        } else {
            ret.away.push(mpc.getPlayerID());
        }
    }
    return ret;
}