const Equip = Java.type("client.inventory.Equip");
const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const MapleInventoryType = Java.type("client.inventory.MapleInventoryType");
const MapleInventoryManipulator = Java.type("server.MapleInventoryManipulator");
/* izarooni */
const features = [];
let feature = null;
let status = 0;

function StopMonsterControls(selection) {
    if (status == 1) {
        cm.sendSimple("#b#L0#Stop Controlling#l\r\n#L1#Begin Controlling#l");
    } else if (status == 2) {
        if (selection == 0) {
            let controls = new java.util.ArrayList(player.getControlledMonsters());
            controls.forEach(c => {
                c.setController(null);
                player.getMap().updateMonsterController(null);
                // player.stopControllingMonster(c);
                // player.announce(MaplePacketCreator.stopControllingMonster(c.getObjectId()))
            });
            cm.sendNext("Stopped controlling monsters");
        } else if (selection == 1) {
            player.getMap().getMonsters().forEach(m => player.announce(MaplePacketCreator.controlMonster(m, false, false)));
            cm.sendNext("Begun controlling monsters");
        }
    } else reset();
}
features.push(new Selector("Update Monster Controllers", StopMonsterControls));

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (feature == null) {
        if (status === 1) {
            let text = "What can I help you with?\r\n#b";
            let i;
            for (i = 0; i < features.length; i++) {
                text += "\r\n#L" + i + "#" + features[i].descriptor + "#l";
            }
            if (i === 0) {
                cm.sendOk("No functions available");
                cm.dispose();
            } else {
                cm.sendSimple(text, 2);
            }
        } else if (status === 2) {
            feature = features[selection].func;
            status = 0;
            action(1, 0, 0);
        }
    } else {
        feature(selection);
    }
}

function reset() {
    status = 0;
    feature = null;
    action(1, 0, 0);
}

function Selector(descriptor, func) {
    this.descriptor = descriptor;
    this.func = func;
}
