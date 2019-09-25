const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const maps = {
    DKerningCity = 103000100,
    DKerningSquare = 103000310,
    SubwayTrain = 103000301,
    KerningTrain  = 103000302
};
var uid = 1;

function init() {
}

function setup() {
	let eim = em.newInstance("KerningTrain_" + (uid++));
    return eim;
}

function playerEntry(eim, player) {
    let trainMap = 0;
    if (player.getMapId() == maps.DKerningCity) {
        trainMap = maps.SubwayTrain;
        eim.setProperty("destination", maps.DKerningSquare);
    } else {
        trainMap = maps.KerningTrain;
        eim.setProperty("destination", maps.DKerningCity);
    }

    player.changeMap(trainMap);
    player.announce(MaplePacketCreator.getClock(10));
    eim.schedule("timeOut", 10 * 1000);
}

function timeOut(eim) {
    eim.getPlayers().forEach(p => {
        p.changeMap(eim.getProperty("destination"))
        p.setEventInstance(null);
    });
    eim.dispose();
}

function playerDisconnected(eim, player) {
}

function disbandParty(eim) {
}

function cancelSchedule() {
    em.getInstances().forEach(i => i.dispose());
}

function dispose(eim) {
    eim.getPlayers().forEach(p => {
        if (p.getMapId() == maps.KerningTrain || p.getMapId() == maps.SubwayTrain) {
            p.changeMap(i.getProperty("destination"))
        }
        p.setEventInstance(null);
    });
}
