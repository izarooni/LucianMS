load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "30 days old";
}

function testForPlayer(player) {
    let ps = Database.getConnection().prepareStatement("select createdate from characters where id = ?");
    ps.setInt(1, player.getId());
    let rs = ps.executeQuery();
    if (rs.next()) {
        let timestamp = rs.getTimestamp("createdate").getTime();
        timestamp = parseInt(timestamp);
        let diff = Date.now() - timestamp;
        let days = ((diff / (1000 * 60 * 60 * 24) % 30));
        if (days >= 30) {
            return true;
        }
    }
    return false;
}

function reward(player) {
    let achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}

function readableRewards(rr) {
}
