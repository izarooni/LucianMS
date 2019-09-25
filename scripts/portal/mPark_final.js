const MonsterPark = Java.type("com.lucianms.features.MonsterPark");

function enter(ms) {

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
            return false;
        }
    }
    return false;
}
