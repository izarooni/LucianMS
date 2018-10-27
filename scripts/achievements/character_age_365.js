load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "365 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    if (days >= 365) {
        return true;
    }
    return false;
}

function reward(player) {
    if (player.getMeso() <= 2144483647) { // int.max_value - 3m
        player.gainMeso(3000000, true);
        let achieve = player.getAchievement(getName());
        achieve.setCompleted(true);
        return true;
    }
    return false;
}

function readableRewards(rr) {
    return rr.add("3,000,000 mesos");
}