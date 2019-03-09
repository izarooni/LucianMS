load('scripts/util_gpq.js');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const TaskExecutor = Java.type('com.lucianms.scheduler.TaskExecutor');

const EventName = "GuildQuest";

function setup() {
    let eim = em.newInstance(`${EventName}-${em.getChannel().getId()}`);

    eim.vars = {
        debug: false,
        canEnter: false, // waiting room

        // stage 1 - 990000300
        statueStage: 0,
        statuePatterns: [],
        enteredPattern: [],

        // stage 3 - 990000500
        wisemanCombo: undefined,
        wisemanAttempt: 1,
    };

    let map = eim.getMapInstance(nFieldWaitingRoom);
    map.getPortal(4).setScriptName("guildwaitingexit");
    map.getPortal(5).setScriptName("guildwaitingenter");

    map = eim.getMapInstance(nFieldWisemenGuards);
    map.setEverlast(true);

    return eim;
}

// -------- custom functions --------

function HitStatue(eim, map, reactor) {
    map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, 0));
    if (eim.vars.debug) {
        print(`[GPQ Statues (GuildQuest.js)] Hitting reactor ${reactor.getId()} / ${reactor.getName()}`);
        let player = map.getCharacterById(eim.vars.playerID);
        map.spawnMesoDrop(10, reactor.getPosition(), reactor, player, false, 0);
    }
}

function CreateStatueCombo(eim) {
    let map = eim.getMapInstance(nFieldGPQStatues);
    let pattern = [];
    for (let i = 0; i < (4 + eim.vars.statueStage); i++) {
        let reactorName = Math.ceil(Math.random() * 20);
        let reactor = map.getReactorByName(`${reactorName}`);
        TaskExecutor.createTask(() => eim.invokeFunction("HitStatue", eim, map, reactor), 5000 + (3500 * i));
        pattern.push(reactorName);
    }
    if (eim.vars.debug) print(`[GPQ Statues (GuildQuest.js)] Created pattern: ${JSON.stringify(pattern)}`);
    eim.vars.statuePatterns.push(pattern);
}

// -------- EventInstanceManager functions --------

function playerEntry(eim, player) {
    player.changeMap(eim.getMapInstance(nFieldWaitingRoom));
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
    return 1;
}

function dispose(eim) {
    eim.getPlayers().forEach(p => {
        eim.removePlayer(p);
    });
    em.removeInstance(eim.getName());
}

function disbandParty(eim) {
}

function clearPQ(eim) {
}

function playerExit(eim, player) {
    player.changeMap(nFieldConstructionSite);

    if (eim.isLeader(player) || eim.getPlayers().isEmpty()) {
        eim.dispose();
    }
}

function allMonstersDead(eim) {
}

// -------- EventManager functions --------

function cancelSchedule() {
    let arr = new java.util.ArrayList(em.getInstances());
    for (let i = 0; i < arr.size(); i++) {
        let eim = arr.get(i);
        eim.dispose();
        print(`Disposed event script instance ${eim.getName()}`);
    }
    arr.clear();
}

// -------- Script functions --------

function init() {
}