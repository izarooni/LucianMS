load('scripts/util_achievements.js');
/* izarooni */

function getRequirement(player) {
    let rbs = player.getRebirths();
    if (rbs < 100) return 25;
    else if (rbs < 500) return 100;
    else if (rbs < 3000) return 250;
    else return 1000; 
}

function getName() {
    return "Reach rebirths";
}

function getDescription(player) {
    return `You will recveive rewards for every ${getRequirement(player)} rebirth`;
}

function testForPlayer(player) {
    let rbs = player.getRebirths();
    if (rbs % getRequirement(player) == 0) {
        reward(player);
    }
    // never mark as complete
    return false;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 6)]);
}

function readableRewards(rr) {
    return rr.add(`6 #z${ServerConstants.CURRENCY}#`);
}
