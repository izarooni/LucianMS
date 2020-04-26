load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "Reach level 200";
}

function testForPlayer(player) {
    return player.getLevel() >= 200;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(0, 1000000, "meso")]);
	if (reward(player)) {
            player.announce(MaplePacketCreator.showEffect("quest/party/clear4"));
            player.announce(MaplePacketCreator.mapSound("customJQ/quest"));
            player.sendMessage("You received 1M Mesos for reaching {} levels");
        }
    }
    // never mark as complete
    return false;
}

function readableRewards(rr) {
    return rr.add(`1,000,000 Mesos`);
}
