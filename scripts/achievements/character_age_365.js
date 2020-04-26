load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "365 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    return days >= 365;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 2)]);
}

function readableRewards(rr) {
    return rr.add(`2x #z${ServerConstants.CURRENCY}#`);
}
