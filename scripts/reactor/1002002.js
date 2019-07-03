const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');

function hit() {
    let reactor = rm.getReactor();
    if (reactor.getMap().getId() != 89) return;

    let state = reactor.getState();
    if (state == 2) { // last visible state in WZ
        if (Math.random() < 0.13) {
            rm.getPlayer().sendMessage(-1, "A small shimmering object falls out of the box...");
            rm.gainItem(ServerConstants.getAutoRebirthItem(), 1, true);
            rm.getPlayer().changeMap(reactor.getMap().getId(), 30);
        } else {
            rm.getPlayer().sendMessage(-1, "The box was destroyed but nothing was found inside...");
        }
        reactor.getMap().sendPacket(reactor.makeDestroyData());
        reactor.setAlive(false);
    }
}