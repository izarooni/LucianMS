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
        var text = "\r\n#b";
        var i = 0;
        player.getMap().getPortals().forEach(function(gp){
            text += "#L" + gp.getId() + "#Portal" + gp.getId() + "#l\r\n";
        });
        cm.sendSimple(text);
    } else if (status == 2) {
        this.portal = player.getMap().getPortal(selection);
        if (this.portal != null) {
            var text = "";
            text += "\r\nID: " + this.portal.getId();
            text += "\r\nName: " + this.portal.getName();
            text += "\r\nScript Name: " + this.portal.getScriptName()
            text += "\r\nTarget Map: " + this.portal.getTargetMapId();
            text += "\r\nPosition: " + this.portal.getPosition().toString();
            cm.sendNext(text);
        } else {
            cm.sendOk("Unable to find specified portal");
            cm.dispose();
        }
    } else if (status == 3) {
        player.changeMap(player.getMap(), this.portal.getPosition());
        cm.dispose();
    }
}