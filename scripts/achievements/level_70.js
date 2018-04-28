/* izarooni */

function getName() {
    return "Reach level 70";
}

function testForPlayer(player) {
    return player.getLevel() >= 70;
}

function reward(player) {
    if (player.getMeso() <= 2145983647) { // int.max_value - 1.5m
        player.gainMeso(1500000, true);
        let achieve = player.getAchievement(getName());
        achieve.setCompleted(true);
        return true;
    }
    return false;
}

function readableRewards(rr) {
    return rr.add("1,500,000 mesos");
}