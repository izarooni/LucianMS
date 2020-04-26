load('scripts/util_achievements.js');
/* izarooni */

function getName() {
    return "Reach level 70";
}

function testForPlayer(player) {
    return player.getLevel() >= 70;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(0, 200000, "meso")]);
	if (reward(player)) {
            player.announce(MaplePacketCreator.showEffect("quest/party/clear4"));
            player.announce(MaplePacketCreator.mapSound("customJQ/quest"));
            player.sendMessage("You received 200K Mesos for reaching {} levels");
        }
    }
    // never mark as complete
    return false;
}

function readableRewards(rr) {
    return rr.add(`2,000,00 Mesos`);
}
