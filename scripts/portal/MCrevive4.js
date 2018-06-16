function enter(pi) {
    let player = pi.getPlayer();
    player.changeMap(pi.getPlayer().getMapId() - 1, player.getTeam() == 0 ? "red_revive" : "blue_revive");
    return true;
}
