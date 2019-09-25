const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const ExpeditionType = Java.type('com.lucianms.server.expeditions.MapleExpeditionType');

const FightTime = 60;
const EventName = "Horntail-" + em.getChannel().getId();
const Fields = {
    TrialOne: 240060000, // Cave of Life - The Cave of Trial I
    TrialTwo: 240060100, // Cave of Life - The Cave of Trial II
    Horntail: 240060200, // Cave of Life - Horntail's Cave
    Entrance: 240050400
};

// -------- EventInstanceManager functions --------

function setup(eim) {
    eim.setProperty("registering", "true");

    eim.schedule("timeOut", 1000 * 60 * (FightTime + 10));
    eim.startEventTimer(1000 * 60 * FightTime);
}

function playerEntry(eim, player) {
    let map = eim.getMapInstance(Fields.TrialOne);
    player.changeMap(map, map.getPortal(0));
}

function moveMap(eim, player, map) {
    if (!map.isInstanced()) {
        eim.unregisterPlayer(player);
        if (eim.getPlayers().isEmpty()) {
            eim.dispose();
        }
    }
    return true;
}

function playerDead(eim, player) {
}

function playerRevive(eim, player) {
    eim.unregisterPlayer(player);

    player.setHp(500);
    player.setStance(0);
    player.changeMap(Fields.Entrance);
    if (eim.isEmpty()) {
        eim.dispose();
    }
    return false;
}

function playerDisconnected(eim, player) {
    let players = eim.getPlayers();
    if (eim.getProperty("leader") == player.getName()) {
        eim.broadcastMessage(6, "The leader of the expedition has disconnected.");
    }
    if (players.isEmpty()) {
        eim.dispose();
    }
}

function monsterValue(eim, player, monster) {
    return 1;
}

function dispose(eim) {
    eim.getEventManager().removeInstance(eim.getName());
}

function disbandParty(eim) {
}

function clearPQ(eim) {
    eim.dispose();
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(Fields.Entrance);
    if (eim.getPlayers().isEmpty()) {
        eim.dispose();
    }
}

function allMonstersDead(eim) {
}

function closeRegistration(eim, map, time) {
    if (time > 0) {
        map.broadcastMessage(MaplePacketCreator.getClock(time / 1000));
        em.schedule("closeRegistration", time, eim, map, 0);
    } else {
        map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Time limit has been reached. Ehe expedition has been disbanded."));
        eim.dispose();
    }
}

function timeout(eim) {
    eim.dispose();
}

// -------- EventManager functions --------

function cancelSchedule() {
}

// -------- Script functions --------

function init() {
    em.setProperty("shuffleReactors", "false");
}