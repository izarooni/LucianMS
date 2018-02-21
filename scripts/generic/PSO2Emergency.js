var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var MapleLifeFactory = Java.type("server.life.MapleLifeFactory");
var TaskExecutor = Java.type("scheduler.TaskExecutor");
var map = ch.getMapFactory().getMap(player.getMapId());
var monsters = [
    {level:20, mID:9400595},
    {level:25, mID:2100103},
    {level:30, mID:3000005},
    {level:35, mID:3230200},
    {level:40, mID:9400517},
    {level:45, mID:4130100},
    {level:50, mID:5120503},
    {level:55, mID:5130101},
    {level:60, mID:9420511},
    {level:65, mID:9420534},
    {level:70, mID:9400640},
    {level:75, mID:7130200},
    {level:80, mID:9400545},
    {level:85, mID:7130010},
    {level:90, mID:8140700},
    {level:95, mID:9895239},
    {level:100, mID:9895240},
    {level:105, mID:8200005},
    {level:110, mID:8190003},
    {level:115, mID:8200008},
    {level:120, mID:8200009},
    {level:125, mID:8200011},
    {level:130, mID:8200012},
    {level:135, mID:8600000},
    //{level:140, mID:},
    //{level:145, mID:},
    {level:150, mID:9400112},
    {level:155, mID:9400113},
    //{level:160, mID:},
    //{level:165, mID:},
    {level:170, mID:8610006},
    //{level:175, mID:},
    //{level:180, mID:},
    //{level:185, mID:},
    {level:190, mID:8620000},
    {level:195, mID:8620007},
    {level:200, mID:8620009},
];

var spawnPoints = map.getMonsterSpawnPoints();
var spawned = 0;
cancelTask = null;

if (!map.isTown() && !spawnPoints.isEmpty()) {
    map.killAllMonsters();
    map.setRespawnEnabled(false);
    cancelTask = TaskExecutor.createTask(function() {
        map.killAllMonsters();
        map.setRespawnEnabled(true);
        map.broadcastMessage(MaplePacketCreator.showEffect("dojang/timeOver"));
    }, 1000 * 60);

    var mselect = null;
    for (var i = 0; i < monsters.length; i++) {
        var ob = monsters[i];
        var next = monsters[i + 1];
        if (next == null || (player.getLevel() >= ob.level && player.getLevel() <= next.level)) {
            mselect = ob;
            break;
        }
    }

    if (mselect != null) {
        var summons =  Math.min(30, spawnPoints.size());
        map.broadcastMessage(MaplePacketCreator.getClock(60));
        map.broadcastMessage(MaplePacketCreator.earnTitleMessage(summons + " monsters at level " + mselect.level + " have appeared!"));
        map.broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/3"));
        map.broadcastMessage(MaplePacketCreator.playSound("PSO2/attack"));
        for (var s = 0; s < summons; s++) {
            var sp = spawnPoints.get(Math.floor(Math.random() * spawnPoints.size()));
            var monster = MapleLifeFactory.getMonster(mselect.mID);
            monster.addListener(function(animationTime) {
                spawned--;
                monsterDeath(monster);
            });
            map.spawnMonsterOnGroudBelow(monster, sp.getPosition());
            spawned++;
        }
    }
}

function monsterDeath(monster) {
    if (spawned == 0) {
        if (cancelTask != null) cancelTask.cancel();
        map.setRespawnEnabled(true);
        map.broadcastMessage(MaplePacketCreator.removeClock());
        map.broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/5"));
        map.broadcastMessage(MaplePacketCreator.playSound("PSO2/completed"));
    } else {
        map.broadcastMessage(MaplePacketCreator.serverNotice(5, "[Emergency] There are " + spawned + " monsters left"));
    }
}
