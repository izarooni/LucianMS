/* izarooni */

function getName() {
    return "Reach level 120";
}

function testForPlayer(player) {
    return player.getLevel() >= 120;
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