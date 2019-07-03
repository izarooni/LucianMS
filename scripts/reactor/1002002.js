function hit() {
    let reactor = rm.getReactor();
    if (reactor.getMap().getId() != 89) return;

    let state = reactor.getState();
    if (state == 4) {
        // rm.dropItem(1302000, 1);
        reactor.getMap().sendPacket(reactor.makeDestroyData());
    }
}