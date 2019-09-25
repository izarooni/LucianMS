const ShenronSummoner = Java.type('com.lucianms.features.summoning.ShenronSummoner');

function start(ms) {
    let ss = new ShenronSummoner();
    ss.registerPlayer(ms.getPlayer());
}