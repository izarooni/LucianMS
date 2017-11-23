load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "90 days old";
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
        if (days >= 90) {
            return true;
        }
    }
    return false;
}

function reward(player) {
    if (player.getMeso() <= 2144483647) { // int.max_value - 3m
        player.gainMeso(3000000, true);
        var achieve = player.getAchievement(getName());
        achieve.setCompleted(true);
        return true;
    }
    return false;
}