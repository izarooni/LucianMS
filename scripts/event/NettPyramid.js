const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const TaskExecutor = Java.type('com.lucianms.scheduler.TaskExecutor');

const EntranceFieldID = 926010000;
const BattlefieldFieldID = 926010100;
const ResultMapFieldID = 926010001;
const ATTR_KILLS = "massacre_hit";
const ATTR_MISS = "massacre_miss";
const ATTR_COOL = "massacre_cool";

const EventName = "NettPyramid";
let ID = 0

function setup() {
    let eim = em.newInstance(`${EventName}-${ID++}`);
    let map = getUsableMap(BattlefieldFieldID);
    if (map != null) {
        // let them respawn so they are registered into this event instance
    }
    eim.vars = {
        endTimestamp: Date.now() + (1000 * 60 * 3), // 3 minutes
        destination: map,
        points: 100
    };
    return eim;
}

// -------- Instanced functions --------

function timeout(eim) {
    let totalEXP = eim.vars.points * 200;
    eim.getPlayers().forEach(p => {
        p.changeMap(ResultMapFieldID);
        p.announce(MaplePacketCreator.getMassacreResult(0, totalEXP));
    });
}

// -------- EventInstanceManager functions --------

function playerEntry(eim, player) {
    if (eim.vars.destination == null) {
        player.dropMessage("Nett's Pyramid is currently busy. Try again another time");
        return eim.dispose();
    }
    player.changeMap(eim.vars.destination);

    let timeLeft = eim.vars.endTimestamp - Date.now();
    player.announce(MaplePacketCreator.getClock(timeLeft / 1000));
    eim.schedule("timeout", timeLeft);
}

function moveMap(eim, player, map) {
    return true;
}

function playerDead(eim, player) {
}

function playerRevive(eim, player) {
    return true;
}

function playerDisconnected(eim, player) {
    eim.removePlayer(player);
}

function monsterValue(eim, player, monster) {
    eim.vars.points += 1;

    let map = em.getChannel().getMap(eim.vars.destination);
    map.broadcastMessage(MaplePacketCreator.increaseMassacreGauge(eim.vars.points));

    let earnedKills = eim.vars.points - 100;

    map.broadcastMessage(MaplePacketCreator.setSessionValue(ATTR_KILLS, `${earnedKills}`));
    if (earnedKills % 5 == 0) {
        let coolPoints = earnedKills / 5;
        map.broadcastMessage(MaplePacketCreator.setSessionValue(ATTR_COOL, `${coolPoints}`));
    }
    return 1;
}

function dispose(eim) {
    eim.getPlayers().forEach(p => {
        eim.removePlayer(p);
    });
    if (eim.vars.destination != null) {
        em.getChannel().removeMap(eim.vars.destination);
    }
}

function disbandParty(eim) {
}

function clearPQ(eim) {
}

function playerExit(eim, player) {
    player.announce(MaplePacketCreator.setSessionValue(ATTR_KILLS, `0`));
    player.announce(MaplePacketCreator.setSessionValue(ATTR_COOL, `0`));
    player.changeMap(EntranceFieldID);

    if ((player.getParty() != null && eim.isLeader(player)) || eim.getPlayers().isEmpty()) {
        eim.dispose();
    }
}

function allMonstersDead(eim) {
}

// -------- EventManager functions --------

function getUsableMap(fieldID) {
    let map = em.getChannel().getMap(fieldID);
    if (map == null) return null;
    if (!map.getCharacters().isEmpty()) {
        if (fieldID >= BattlefieldFieldID + 1400) return null;
        return getUsableMap(fieldID + 100);
    }
    map.killAllMonsters();
    map.clearDrops();
    return map.getId();
}

function cancelSchedule() {
}

// -------- Script functions --------

function init() {
}