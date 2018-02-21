function enter(pi) {
    var eim = pi.getPlayer().getEventInstance();
    if (eim != null && eim.getProperty("head1") == null) {
        eim.setProperty("head1", "false");
        pi.getPlayer().getMap().spawnMonsterOnGroudBelow(8810000, 970, 225);
    }
    return true;
}
