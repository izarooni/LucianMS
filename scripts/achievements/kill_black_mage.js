/* izarooni */
let _monsterId = 9895253;
let _killRequire = 1;

function getName() {
    return "Kill the Black Mage";
}

function testForKill(player, monsterId) {
    if (monsterId == _monsterId) {
        let achieve = player.getAchievement(getName());
        let current = achieve.getMonstersKilled() + 1;
        if (current >= _killRequire) {
            achieve.setMonstersKilled(current);
            return true;
        }
    }
    return false;
}

function reward(player) {
    let achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}

function readableRewards(rr) {
}
