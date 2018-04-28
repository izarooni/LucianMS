/* izarooni */

function getName() {
    return "Reach level 200";
}

function testForPlayer(player) {
    return player.getLevel() >= 200;
}

function reward(player) {
    if (player.getMeso() <= 2142483647) { // int.max_value - 5m
        player.gainMeso(5000000, true);
        let achieve = player.getAchievement(getName());
        achieve.setCompleted(true);
        return true;
    }
    return false;
}

function readableRewards(rr) {
    return rr.add("5,000,000 mesos");
}