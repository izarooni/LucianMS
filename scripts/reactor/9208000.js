function act() {
    let eim = rm.getPlayer().getEventInstance();
    if (eim == null) return;
    let reactor = rm.getReactor();
    let reactorName = reactor.getName();

    eim.vars.enteredPattern.push(reactorName);
}