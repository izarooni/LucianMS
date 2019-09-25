/*
 * @Author Stereo
 * @Modified By XkelvinchiaX(Kelvin)
 * @Modified By Moogra
 * @Modified By SharpAceX(Alan)
 * Zakum Battle
 */

var exitMap;
var altarMap;
var minPlayers = 1;
var fightTime = 75;
var altarTime = 15;
var gate;

function init() {
    em.setProperty("shuffleReactors","false");
	exitMap = em.getChannel().getMap(211042300);
	altarMap = em.getChannel().getMap(280030000);// Last Mission: Zakum's Altar
	gate = exitMap.getReactorByName("gate");
	gate.setState(0);//Open gate
}

function setup() {
    var eim = em.newInstance("ZakumBattle_" + em.getProperty("channel"));
	var timer = 1000 * 60 * fightTime;
	eim.setProperty("summoned", "false");
    em.schedule("timeOut", eim, timer);
	em.schedule("altarTimeOut", eim, 1000 * 60 * altarTime);
    eim.startEventTimer(timer);
	gate.setState(1);//Close gate
	return eim;
}

function playerEntry(eim,player) {
	var altar = eim.getMapInstance(altarMap.getId());
    player.changeMap(altar, altar.getPortal(0));

	player.dropMessage(5, "The Zakum Shrine will close if you do not summon Zakum in " + altarTime + " minutes.");
    if (altarMap == null)
        debug(eim, "The altar map was not properly linked.");
}

function playerRevive(eim,player) {
    player.setHp(500);
    player.setStance(0);
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, exitMap.getPortal(0));
    var exped = eim.getPlayers();
    if (exped.size() < minPlayers)
        end(eim,"There are not enough players remaining, the Zakum battle is over.");
    return false;
}

function playerDead(eim,player) {
}

function playerDisconnected(eim,player) {
    var exped = eim.getPlayers();
    if (player.getName().equals(eim.getProperty("leader"))) {
        var iter = exped.iterator();
        while (iter.hasNext()) {
            iter.next().getPlayer().dropMessage(6, "The leader of the expedition has disconnected.");
		}
    }
    //If the expedition is too small.
    if (exped.size() < minPlayers) {
        end(eim,"There are not enough players remaining. The Battle is over.");
    }
}

function monsterValue(eim,mobId) { // potentially display time of death? does not seem to work
    if (mobId == 8800002) { // 3rd body
        var iter = eim.getPlayers().iterator();
        while (iter.hasNext()) {
            iter.next().dropMessage(6, "Congratulations on defeating Zakum!");
		}
    }
    return -1;
}

function leftParty(eim,player) { // do nothing in Zakum
}

function disbandParty(eim) { // do nothing in Zakum
}

function playerExit(eim,player) {
    eim.unregisterPlayer(player);
    player.changeMap(exitMap, exitMap.getPortal(0));
    if (eim.getPlayers().size() < minPlayers) {//not enough after someone left
        end(eim, "There are no longer enough players to continue, and those remaining shall be warped out.");
	}
}

function end(eim,msg) {
    var iter = eim.getPlayers().iterator();
    while (iter.hasNext()) {
        var player = iter.next();
        player.getPlayer().dropMessage(6,msg);
        eim.unregisterPlayer(player);
        if (player != null){
            player.changeMap(exitMap, exitMap.getPortal(0));
		}
    }
	gate.setState(0);//Open gate
    eim.dispose();
}

function removePlayer(eim,player) {
    eim.unregisterPlayer(player);
    player.getMap().removePlayer(player);
    player.setMap(exitMap);
}

function clearPQ(eim) { //When the hell does this get executed?
    end(eim,"As the sound of battle fades away, you feel strangely unsatisfied.");

}

function finish(eim) {
    var iter = eim.getPlayers().iterator();
    while (iter.hasNext()) {
        var player = iter.next();
        eim.unregisterPlayer(player);
        player.changeMap(exitMap, exitMap.getPortal(0));
    }
    eim.dispose();
}

function allMonstersDead(eim) {
}

function dispose(eim) {
}

function altarTimeOut(eim) {
    if (eim != null && eim.getProperty("summoned").equals("false")) {
        if (eim.getPlayerCount() > 0) {
            var pIter = eim.getPlayers().iterator();
            while (pIter.hasNext()){
				var player = pIter.next();
				player.dropMessage(6, "The Shrine has closed since you did not summon Zakum within " + altarTime + " minutes.");
                playerExit(eim, player);
			}
        }
        eim.dispose();
    }
}

function timeOut(eim) {
    if (eim != null) {
        if (eim.getPlayerCount() > 0) {
            var pIter = eim.getPlayers().iterator();
            while (pIter.hasNext()){
				var player = pIter.next();
				player.dropMessage(6, "You have run out of time to defeat Zakum!");
                playerExit(eim, player);
			}
        }
        eim.dispose();
    }
}

function debug(eim,msg) {
    var iter = eim.getPlayers().iterator();
    while (iter.hasNext()) {
        iter.next().getClient().getSession().write(Packages.tools.MaplePacketCreator.serverNotice(6,msg));
    }
}
