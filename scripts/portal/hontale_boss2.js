const Listener = Packages.com.lucianms.server.life.MonsterListener;

function enter(pi) {
    let eim = pi.getPlayer().getEventInstance();
    if (eim != null && eim.getProperty("head2") == null || pi.getPlayer().isDebug()) {
        eim.setProperty("head2", "false");
        let mob = pi.getPlayer().getMap().spawnMonsterOnGroudBelow(8810001, -332, 260);
        let nListener = { monsterKilled: function (ani) { eim.setProperty("head2", "yes") } };
        mob.getListeners().add(new Listener(nListener));
    }
    return true;
}
