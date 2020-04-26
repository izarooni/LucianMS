load('scripts/util_achievements.js');
const TutorialFieldID = 405;
/* izarooni */

function getName() {
    return "Completing the Tutorial";
}

function testForPlayer(player) {
    return player.getMap().getId() == TutorialFieldID;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(0, 150000, "meso")]);
	if (reward(player)) {
            player.announce(MaplePacketCreator.showEffect("quest/party/clear4"));
            player.announce(MaplePacketCreator.mapSound("customJQ/quest"));
            player.sendMessage("You received 150K for completing the Tutorial");
        }
    }
    // never mark as complete
    return false;
}

function readableRewards(rr) {
    return rr.add(`1,500,00 Mesos`);
}
