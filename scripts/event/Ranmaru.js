const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const Listener = Java.type('com.lucianms.server.life.MonsterListener');
const Point    = Java.type('java.awt.Point');

const Fields = { Battle: 807300110 };
const Monsters = { Ranmaru: 9421581 };

const EventName = "Ranmaru-" + em.getChannel().getId();
// -------- EventInstanceManager functions --------

function setup() {
    let instance = em.newInstance(EventName);

    let map = instance.getMapInstance(Fields.Battle, function(fb) {
        return fb.loadPortals().loadFootholds().build();
    });
    map.getPortals().forEach(p => p.setPortalStatus(false));
    let monster = map.spawnMonsterOnGroudBelow(Monsters.Ranmaru, -424, 123);
    instance.registerMonster(monster);    

    return instance;
}

function playerEntry(eim, player) {
    let dest = eim.getMapInstance(Fields.Battle);
    player.changeMap(dest, new Point(-639, 123));
}

function moveMap(eim, player, map) {
    let instanceMap = eim.getMapInstance(Fields.Battle);
    print("match1: " + (player.getMap() == instanceMap));
    print("match2: " + (map != instanceMap));
    if (player.getMap() == instanceMap && map != instanceMap) {
        eim.removePlayer(player);
        return false;
    }
    return true;
}

function playerDead(eim, player) {
}

function playerRevive(eim, player) {
    player.setHp(50);
    player.changeMap(player.getMap(), new Point(-781, -169));
    return false;
}

function playerDisconnected(eim, player) {
}

function monsterValue(eim, player, monster) {
    if (monster.getId() == Monsters.Ranmaru) {
        eim.finishPQ();
    }
    return 1;
}

function dispose(eim) {
    em.removeInstance(EventName);
}

function disbandParty(eim) {
}

function clearPQ(eim) {
    let map = eim.getMapInstance(Fields.Battle);
    map.broadcastMessage(MaplePacketCreator.getClock(5));
    eim.schedule("warpOut", 5000);
}

function warpOut(eim) {
    eim.dispose();
}

function playerExit(eim, player) {
    player.changeMap(Fields.Battle);
    print(eim.getPlayers().isEmpty());
    if (eim.getPlayers().isEmpty()) {
        eim.dispose();
    }
}

function allMonstersDead(eim) {
}

// -------- EventManager functions --------

function cancelSchedule() {
    em.getInstances().forEach(eim => eim.dispose());
}

// -------- Script functions --------

function init() {
}