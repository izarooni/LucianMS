load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "Win both casino games";
}

function testForPlayer(player) {
    let achieve = player.getAchievement(getName());
    return achieve.isCasino1Completed() && achieve.isCasino2Completed();
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 1)]);
}

function readableRewards(rr) {
    return rr.add(`1x #z${ServerConstants.CURRENCY}#`);
}
