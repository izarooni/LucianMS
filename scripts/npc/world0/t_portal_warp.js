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
            text += "\r\n#L0#ID: " + this.portal.getId() + "#l";
            text += "\r\n#L1#Name: " + this.portal.getName() + "#l";
            text += "\r\n#L2#Script Name: " + this.portal.getScriptName() + "#l"
            text += "\r\n#L3#Target Map: " + this.portal.getTargetMapId() + "#l";
            text += "\r\n#L4#Position: " + this.portal.getPosition().toString() + "#l";
            cm.sendSimple(text);
        } else {
            cm.sendOk("Unable to find specified portal");
            cm.dispose();
        }
    } else if (status == 3) {
        if (selection == 2) {
            Packages.com.lucianms.io.scripting.portal.PortalScriptManager.executePortalScript(client, this.portal);
            cm.sendOk("Executed");
        } else if (selection == 4) {
            player.changeMap(player.getMap(), this.portal.getPosition());
        }
        cm.dispose();
    }
}