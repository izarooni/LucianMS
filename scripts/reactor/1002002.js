const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const Occupation = Java.type('com.lucianms.client.meta.Occupation');
var ITEM_DURATION = (1000 * 60 * 60 * 10); // 10 hours

function hit() {
    let reactor = rm.getReactor();
    if (reactor.getMap().getId() != 89) return;

    let state = reactor.getState();
    if (state == 2) { // last visible state in WZ
        if (Math.random() < 0.13) {
            rm.getPlayer().sendMessage(-1, "A small shimmering object falls out of the box...");
            if (rm.getPlayer().getOccupation().getType() == Occupation.Type.Trainer) {
                ITEM_DURATION *= 1.5;
            }
            rm.gainItem(ServerConstants.getAutoRebirthItem(), 1, false, true, ITEM_DURATION);
            rm.getPlayer().changeMap(reactor.getMap().getId(), 30);
        } else {
            rm.getPlayer().sendMessage(-1, "The box was destroyed but nothing was found inside...");
        }
        reactor.getMap().sendPacket(reactor.makeDestroyData());
        reactor.setAlive(false);
    }
}