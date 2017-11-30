var MonsterPark = Java.type("server.events.custom.MonsterPark");

function start(ms) {

    // get monster park event handler
    this.optional = ms.getPlayer().getGenericEvents().stream().filter(function(e){
        return (e instanceof MonsterPark);
    }).findFirst();

    // if one exists in the player instance
    if (this.optional.isPresent()) {
        // and the portal status is enabled
        if (ms.getPortal().getPortalStatus()) {
            // proceed  to the next stage of the monster park
            this.optional.get().advanceMap(ms.getPlayer());
            return true;
        }
    } else {
        ms.getPlayer().sendMessage("You may not use this portal");
    }
    return false;
}
