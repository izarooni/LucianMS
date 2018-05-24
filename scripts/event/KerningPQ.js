/* izarooni

-- Ligator drops
insert into drop_data values (default, 9300000, 4001008, 1, 1, 0, 999999);
insert into drop_data values (default, 9300001, 4001007, 1, 1, 0, 999999);
insert into drop_data values (default, 9300002, 4001008, 1, 1, 0, 999999);
insert into drop_data values (default, 9300003, 4001008, 1, 1, 0, 999999);
*/

importPackage(Packages.world);
var exitMap;
var minPlayers = 3;

function init() { // Initial loading.
    exitMap = em.getChannel().getMapFactory().getMap(103000890);
    em.setProperty("KPQOpen", "true"); // allows entrance.
    em.setProperty("shuffleReactors", "true");
    instanceId = 1;
}

function monsterValue(eim, player, monster) {
    return 1;
}

function setup() { // Invoked from "EventManager.startInstance()"
    var eim = em.newInstance("KerningPQ"); // adds a new instance and returns EventInstanceManager.
    var eventTime = 30 * (1000 * 60); // 30 mins.
    var firstPortal = eim.getMapInstance(103000800).getPortal("next00");
	respawn(eim);
    firstPortal.setScriptName("kpq0");
    em.schedule("timeOut", eim, eventTime); // invokes "timeOut" in how ever many seconds.
    eim.startEventTimer(eventTime); // Sends a clock packet and tags a timer to the players.
    return eim; // returns the new instance.
}

function playerEntry(eim, player) { // this gets looped for every player in the party.
    var map = eim.getMapInstance(103000800);
    player.changeMap(map, map.getPortal(0)); // We're now in KPQ :D
}

function playerDead(eim, player) {
}

function playerRevive(eim, player) { // player presses ok on the death pop up.
    var party = eim.getPlayers().toArray();
    if (eim.isLeader(player) || party.length <= minPlayers) { // Check for party leader
        for (var i = 0; i < party.length; i++)
            playerExit(eim, party[i]);
        eim.dispose();
    } else
        playerExit(eim, player);
}


// function respawn(eim) {
// 	var map = eim.getMapInstance(103000800);
// 	var map2 = eim.getMapInstance(103000805);
// 	if (map.getSummonState()) {	//Map spawns are set to true by default
// 		map.instanceMapRespawn();
// 	}
// 	if(map2.getSummonState()) {
// 		map2.instanceMapRespawn();
// 	}
// 	eim.schedule("respawn", 10000);
// }

function playerDisconnected(eim, player) {
    var party = eim.getPlayers().toArray();
    if (eim.isLeader(player) || party.length < minPlayers) {
        for (var i = 0; i < party.size(); i++)
            if (party[i].equals(player)) {
                removePlayer(eim, player);
            } else {
                playerExit(eim, party[i]);
            }
        eim.dispose();
    } else {
        removePlayer(eim, player);
    }
}

function leftParty(eim, player) {
    var party = eim.getPlayers().toArray();
    if (party.length < minPlayers) {
        for (var i = 0; i < party.length; i++) {
            playerExit(eim, party[i]);
        }
        eim.dispose();
    } else {
        playerExit(eim, player);
    }
}

function disbandParty(eim) {
    var party = eim.getPlayers().toArray();
    for (var i = 0; i < party.length; i++) {
        playerExit(eim, party[i]);
    }
    eim.dispose();
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, exitMap.getPortal(0));
}

function removePlayer(eim, player) {
    eim.unregisterPlayer(player);
    player.getMap().removePlayer(player);
    player.setMap(exitMap);
}

function clearPQ(eim) {
    var party = eim.getPlayers().toArray();
    for (var i = 0; i < party.length; i++)
        playerExit(eim, party[i]);
    eim.dispose();
}

function allMonstersDead(eim) {
}

function dispose(eim) {
	em.cancelSchedule();
    em.schedule("OpenKPQ", 10000);
}

function OpenKPQ() {
    em.setProperty("KPQOpen", "true");
}

function timeOut(eim) {
    if (eim != null) {
        if (eim.getPlayerCount() > 0) {
            var pIter = eim.getPlayers().iterator();
            while (pIter.hasNext()) {
                playerExit(eim, pIter.next());
            }
        }
        eim.dispose();
    }
}
