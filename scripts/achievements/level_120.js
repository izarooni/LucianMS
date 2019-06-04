load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "Reach level 120";
}

function testForPlayer(player) {
    return player.getLevel() >= 120;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 1)]);
}

function readableRewards(rr) {
    return rr.add(`1x #z${ServerConstants.CURRENCY}#`);
}
