load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "90 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    timestamp = parseInt(timestamp);
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    if (days >= 90) {
        return true;
    }
    return false;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 1)]);
}

function readableRewards(rr) {
    return rr.add(`1x #z${ServerConstants.CURRENCY}#`);
}
