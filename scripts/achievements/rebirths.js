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
    if (rbs < 1) return false;
    let achieve = player.getAchievement(getName());
    if (achieve.getState() < rbs && rbs % getRequirement(player) == 0) {
        if (reward(player)) {
            achieve.setState(rbs);
            player.announce(MaplePacketCreator.showEffect("quest/party/clear4"));
            player.announce(MaplePacketCreator.mapSound("customJQ/quest"));
            player.sendMessage("You received 5M Mesos for reaching {} rebirths", rbs);
        }
    }
    // never mark as complete
    return false;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(0, 5000000, "meso")]);
}

function readableRewards(rr) {
    return rr.add(`5,000,000 Mesos`);
}
