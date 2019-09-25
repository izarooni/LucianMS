function enter(pi) {
    pi.warp(200090010, 5);
    if (pi.getPlayer().getClient().getEventManager("Boats").getProperty("haveBalrog").equals("true")) {
        pi.changeMusic("Bgm04/ArabPirate");
    }
    return true;
}
