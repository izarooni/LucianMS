// 4001101 - Rice Cake
// Significant monsters
// 9300061 - Bunny
// 9300062 - Flyeye
// 9300063 - Stirge
// 9300064 - Goblin Fires
// Significant NPCs
// 1012112 - Troy
// 1012113 - Tommy
// 1012114 - Growlie
// map effects
// Map/Obj/Effect/quest/gate/3 - warp activation glow
// quest/party/clear - CLEAR text
// Party1/Clear - clear sound
// INSERT monsterdrops (monsterid,itemid,chance) VALUES (9300061,4001101,1);
var M_ExitFail = 910010400;
var M_ExitSuccess = 910010100;
var M_Stage = 910010000;

var T_Deadline = 10;

function init() {
    em.setProperty("HPQOpen", "true");
}

function monsterValue(eim, player, monster) {
    return 1;
}

function setup() {
    em.setProperty("HPQOpen", "false")
    var eim = em.newInstance("HenesysPQ_" + em.getProperty("LeaderName"));
    eim.setProperty("clear", "false");
    eim.setProperty("ActivatedPlants", "0");
    eim.getMapInstance(M_Stage).setSummonState(false);
    eim.getMapInstance(M_Stage).killAllMonsters();
    var timer = 1000 * 60 * T_Deadline;
    eim.schedule("disbandParty", timer);
    eim.startEventTimer(timer);
    return eim;
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(M_Stage);
    player.changeMap(map, map.getPortal(0));
}

function playerDead(eim, player) {
    if (eim.isLeader(player)) {
        eim.getPlayers().forEach(function(chr) {
            playerExit(eim, chr);
        });
        eim.dispose();
    }
}

function playerDisconnected(eim, player) {
    playerDead(eim, player);
}

function leftParty(eim, player) {
    if (eim.isLeader(player)) {
        playerDead(eim, player);
    } else {
        playerExit(eim, player);
    }
}

function disbandParty(eim) {
    eim.getPlayers().forEach(function(chr) {
        playerExit(eim, chr);
    });
    eim.dispose();
}

function playerExitClear(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(M_ExitSuccess);
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(M_ExitFail);
}

function removePlayer(eim, player) {
    eim.unregisterPlayer(player);
    player.getMap().removePlayer(player);
    player.setMap(exitMap);
}

function clearPQ(eim) {
    eim.getPlayers().forEach(function(chr) {
        playerExitClear(eim, party.get(i));
    });
    eim.dispose();
}

function allMonstersDead(eim) {}

function dispose() {
    em.schedule("OpenHPQ", 5000);
}

function OpenHPQ() {
    em.setProperty("HPQOpen", "true");
}
