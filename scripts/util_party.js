function partyExists() {
    return cm.getParty() != null;
}

function partySize() {
    return cm.getParty().getMembers().size();
}

function membersPresent(m) {
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