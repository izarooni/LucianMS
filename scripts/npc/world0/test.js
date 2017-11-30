/* izarooni */
var status = 0;
var maps = [
    953020000,
    953090000,
    953080000,
    954010000,
    954030000,
    953050000,
    954050000,
    953030000,
    953060000,
    953040000,
    954040000
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        for (var i = 0; i < maps.length; i++) {
            var mapid = maps[i];
            var levels = [];
            for (var a = 0; a < 6; a++) {
                client.getChannelServer().getMapFactory().reloadField(mapid + (100 * a));
                var map = client.getChannelServer().getMapFactory().getMap(mapid + (100 * a));
                if (map != null) {
                    map.getMonsterSpawnPoints().forEach(function(sp) {
                        levels.push(sp.getMonster().getLevel());
                    });
                }
            }
            print("[" + mapid + "] " + client.getChannelServer().getMapFactory().getMap(mapid).getMapName() + " // Average monster level: " + getAverage(levels));
        }
        cm.dispose();
    }
}

function getAverage(arr) {
    if (arr.length == 0) return "0";
    var sum = 0;
    for (var i = arr.length - 1; i > 0; i--) {
        sum += arr[i];
    }
    return Math.floor(sum / arr.length);
}
