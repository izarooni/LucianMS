/* izarooni */

function getName() {
    return "Win both casino games";
}

function testForPlayer(player) {
    let achieve = player.getAchievement(getName());
    return achieve.isCasino1Completed() && achieve.isCasino2Completed();
}

function reward(player) {
    let achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}

function readableRewards(rr) {
}
