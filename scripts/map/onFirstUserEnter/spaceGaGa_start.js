const MaplePacketCreator = Java.type('tools.MaplePacketCreator');

function start(ms) {
    let player = ms.getPlayer();
    player.resetEnteredScript();
    ms.getClient().announce(MaplePacketCreator.showEffect("event/space/start"));
    player.startMapEffect("Please rescue Gaga within the time limit.", 5120027);
    let map = player.getMap();
    if (map.getTimeLeft() > 0) {
        ms.getClient().getSession().write(MaplePacketCreator.getClock(map.getTimeLeft()));
    } else {
        map.addMapTimer(180);
    }
    ms.useItem(2360002); // HOORAY <3
}  