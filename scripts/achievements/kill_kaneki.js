load('scripts/util_achievements.js');
/* izarooni */
let MonsterID = 9895251;
let KillCount = 1;

function getName() {
    return "Kill Kaneki";
}

function testForKill(player, monsterId) {
    if (monsterId == MonsterID) {
        let achieve = player.getAchievement(getName());
        let current = achieve.getMonstersKilled() + 1;
        if (current >= KillCount) {
            achieve.setMonstersKilled(current);
            return true;
        }
    }
    return false;
}

function reward(player) {
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 5)]);
}

function readableRewards(rr) {
    return rr.add(`5x #z${ServerConstants.CURRENCY}#`);
}
