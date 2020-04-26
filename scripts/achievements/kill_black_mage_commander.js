load('scripts/util_achievements.js');
/* izarooni */
let MonsterID = 9895257;
let KillCount = 1;

function getName() {
    return "Kill the Black Mage Commander";
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
    return tryGiveItem(player, [new RewardItem(ServerConstants.CURRENCY, 1)]);
}

function readableRewards(rr) {
    return rr.add(`1x #z${ServerConstants.CURRENCY}#`);
}
