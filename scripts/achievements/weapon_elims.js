load('scripts/util_achievements.js');
/* izarooni */
const Rewards = [500, 1500, 3000, 4500, 5000, 6999, 10000];

function getName() {
    return "Attain monster eliminations";
}

function testForPlayer(player) {
    let weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem(-11);
    if (weapon !=  null) {
        for (let i = 0; i < Rewards.length; i++) {
            if (weapon.getItemLevel() == i && weapon.getEliminations() == Rewards[i]) {
                weapon.setItemLevel(weapon.getItemLevel() + 1);
                player.announce(MaplePacketCreator.showEquipmentLevelUp());
                return true;
            }
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
