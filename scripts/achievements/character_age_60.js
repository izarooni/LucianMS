load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "60 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    timestamp = parseInt(timestamp);
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    return days >= 60;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 1)]);
}

function readableRewards(rr) {
    return rr.add(`1x #z${ServerConstants.CURRENCY}#`);
}
