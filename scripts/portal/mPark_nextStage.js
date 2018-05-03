var MonsterPark = Java.type("com.lucianms.features.MonsterPark");

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
            return true;
        } else {
            ms.getPlayer().sendMessage("You must defeat all the monsters in the map before you proceed to the next stage");
        }
    } else {
        ms.getPlayer().sendMessage("You may not use this portal");
    }
    return false;
}
