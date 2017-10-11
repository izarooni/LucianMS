var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
/* izarooni */
var status = 0;
var error = "";
var effects = [
    ["PQ - Clear", "quest/party/clear"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var text = "You want to view an effect?\r\n#b";
        text += "#L" + effects.length + "#Custom effect#l\r\n";
        for (var i = 0; i < effects.length; i++) {
            text += "\r\n#L" + i + "#" + effects[i][0] + "#l";
        }
        cm.sendSimple(text);
    } else if (status == 2) {
        if (this.choice == null) {
            this.choice = selection;
        }
        if (this.choice >= 0 && this.choice < effects.length) {
            player.announce(MaplePacketCreator.showEffect(effects[this.choice][1]));
            cm.dispose();
        } else {
            cm.sendGetText(error + "\r\nAbsolutely you can enter your own effect path!");
        }
    } else if (status == 3) {
        var effect = cm.getText();
        if (effect == null || effect.length() == 0) {
            error = "#e#rYou can't enter a blank message!#k#n";
            status = 1;
            action(1, 0, 0);
            return;
        } else {
            player.announce(MaplePacketCreator.showEffect(effect));
        }
        cm.dispose();
    }
}