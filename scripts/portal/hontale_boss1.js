const Listener = Packages.com.lucianms.server.life.MonsterListener;

function enter(pi) {
    let eim = pi.getPlayer().getEventInstance();
    if (eim != null && eim.getProperty("head1") == null || pi.getPlayer().isDebug()) {
        eim.setProperty("head1", "false");
        let mob = pi.getPlayer().getMap().spawnMonsterOnGroudBelow(8810000, 970, 225);
        let nListener = { monsterKilled: function (ani) { eim.setProperty("head1", "yes") } };
        mob.getListeners().add(new Listener(nListener));
    }
    return true;
}
