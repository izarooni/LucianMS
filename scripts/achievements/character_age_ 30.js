load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "30 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    timestamp = parseInt(timestamp);
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    return days >= 30;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 10)]);
}

function readableRewards(rr) {
    return rr.add(`10x #z${ServerConstants.CURRENCY}#`);
}
