const ObjectType = Java.type("server.maps.MapleMapObjectType");
/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Which NPC would you like to go to?\r\n#b";
        player.getMap().getMapObjects().stream().filter(o => o.getType() == ObjectType.NPC).forEach(o => {
            content += "\r\n#L" + o.getObjectId() + "##p" + o.getId() + "##l";
        });
        cm.sendSimple(content);
    } else if (status == 2) {
        this.obj = player.getMap().getMapObject(selection);
        if (this.obj != null) {
            cm.setNpc(this.obj.getId());
            cm.sendNext("Are you sure you want to warp to me?")
        } else {
            cm.sendOk("What?");
            cm.dispose();
        }
    } else if (status == 3) {
        player.changeMap(player.getMap(), this.obj.getPosition());
        cm.dispose();
    }
}
