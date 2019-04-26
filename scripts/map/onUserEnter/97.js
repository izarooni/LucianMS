const BlackMageSummoner = Java.type('com.lucianms.features.summoning.BlackMageSummoner');

function start(ms) {
    let ss = new BlackMageSummoner();
    ss.registerPlayer(ms.getPlayer());
}