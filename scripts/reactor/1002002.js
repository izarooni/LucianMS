const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');

function hit() {
    let reactor = rm.getReactor();
    if (reactor.getMap().getId() != 89) return;

    let state = reactor.getState();
    if (state == 4) { // last visible state in WZ
        rm.dropItem(ServerConstants.getAutoRebirthItem(), 1);
        reactor.getMap().sendPacket(reactor.makeDestroyData());
        reactor.setAlive(false);
    }
}