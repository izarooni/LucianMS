load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "Reach level 200";
}

function testForPlayer(player) {
    return player.getLevel() >= 200;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 2)]);
}

function readableRewards(rr) {
    return rr.add(`2x #z${ServerConstants.CURRENCY}#`);
}
