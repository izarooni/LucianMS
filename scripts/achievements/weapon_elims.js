const MapleInventoryType = Java.type("client.inventory.MapleInventoryType");
/* izarooni */
const Rewards = [500, 1500, 3000, 4500, 5000, 6999, 10000];

function getName() {
    return "Attain monster eliminations";
}

function testForPlayer(player) {
    let weapon = getWeapon(player);
    if (weapon !=  null) {
        for (let i = 0; i < Rewards.length; i++) {
            if (weapon.getItemLevel() == i && weapon.getEliminations() == Rewards[i]) {
                weapon.setItemLevel(weapon.getItemLevel() + 1);
                return true;
            }
        }
    }
    return false;
}

function reward(player) {
    let achieve = player.getAchievement(getName());
    return true;
}

function getWeapon(player) {
    return player.getInventory(MapleInventoryType.EQUIPPED).getItem(-11);
}

function readableRewards(rr) {
}
