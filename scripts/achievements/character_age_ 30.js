load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "30 days old";
}

function testForPlayer(player) {
    var ps = Database.getConnection().prepareStatement("select createdate from characters where id = ?");
    ps.setInt(1, player.getId());
    var rs = ps.executeQuery();
    if (rs.next()) {
        var timestamp = rs.getTimestamp("createdate").getTime();
        timestamp = parseInt(timestamp);
        var diff = Date.now() - timestamp;
        var days = ((diff / (1000 * 60 * 60 * 24) % 30));
        if (days >= 30) {
            return true;
        }
    }
    return false;
}

function reward(player) {
    var achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}