const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const OuterSpaceFieldID = 98;
/* izarooni */

function getName() {
    return "Discord the Outer Space, Planet Chirithy map";
}

function testForPlayer(player) {
    return player.getMap().getId() == OuterSpaceFieldID;
}

function reward(player) {
    player.announce(MaplePacketCreator.showEffect("quest/party/clear2"));
    let achieve = player.getAchievement(getName());
    achieve.setCompleted(true);
    return true;
}

function readableRewards(rr) {
}