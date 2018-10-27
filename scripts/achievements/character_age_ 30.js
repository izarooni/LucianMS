load('scripts/util_imports.js');
/* izarooni */

function getName() {
    return "30 days old";
}

function testForPlayer(player) {
    let timestamp = player.getCreateDate();
    timestamp = parseInt(timestamp);
    let diff = Date.now() - timestamp;
    let days = (diff / (1000 * 60 * 60 * 24));
    if (days >= 30) {
        return true;
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
