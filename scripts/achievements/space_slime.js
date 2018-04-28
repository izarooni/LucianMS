/* izarooni */
let _monsterId = 9895259;
let _killRequire = 1;

function getName() {
    return "Kill the Space Slime";
}

function testForKill(player, monsterId) {
    if (monsterId == _monsterId) {
        let achieve = player.getAchievement(getName());
        let current = achieve.getMonstersKilled() + 1;
        if (current >= _killRequire) {
            acheive.setMonstersKilled(current);
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
