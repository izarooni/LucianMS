/* izarooni */

function getName() {
    return "Win both casino games";
}

function testForPlayer(player) {
    var achieve = player.getAchievement(getName());
    return achieve.isCasino1Completed() && achieve.isCasino2Completed();
}

function reward(player) {
    var achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}