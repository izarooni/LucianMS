/* izarooni */
var _monsterId = 9895251;
var _killRequire = 1;

function getName() {
    return "Kill Kaneki";
}

function testForKill(player, monsterId) {
    if (monsterId == _monsterId) {
        var achieve = player.getAchievement(getName());
        var current = achieve.getMonstersKilled() + 1;
        if (current >= _killRequire) {
            acheive.setMonstersKilled(current);
            return true;
        }
    }
    return false;
}

function reward(player) {
    var achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}