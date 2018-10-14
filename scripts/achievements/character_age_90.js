load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "90 days old";
}

function testForPlayer(player) {
    let con = Database.getConnection();
    try {
        let ps = con.prepareStatement("select createdate from characters where id = ?");
        ps.setInt(1, player.getId());
        let rs = ps.executeQuery();
        if (rs.next()) {
            let timestamp = rs.getTimestamp("createdate").getTime();
            timestamp = parseInt(timestamp);
            let diff = Date.now() - timestamp;
            let days = (diff / (1000 * 60 * 60 * 24));
            if (days >= 90) {
                return true;
            }
        }
    } finally { con.close(); }
    return false;
}

function reward(player) {
    if (player.getMeso() <= 2144483647) { // int.max_value - 3m
        player.gainMeso(3000000, true);
        let achieve = player.getAchievement(getName());
        achieve.setCompleted(true);
        return true;
    }
    player.sendMessage(`Unable to receive achievement reward '${getName()}' due to mesos overflow`);
    return false;
}

function readableRewards(rr) {
    return rr.add("3,000,000 mesos");
}