const Equip = Java.type("client.inventory.Equip");
const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const MapleInventoryType = Java.type("client.inventory.MapleInventoryType");
const MapleInventoryManipulator = Java.type("server.MapleInventoryManipulator");
/* izarooni */
const features = [];
let feature = null;
let status = 0;

function ListMonsters(selection) {
    if (status == 1) {
        let content = "";
        let objects = player.getMap().getMapObjects();
        for (let i = 0; i < objects.size(); i++) {
            let obj = player.getMap().getMonsterByOid(objects.get(i).getObjectId());
            if (obj != null) {
                content += `\r\n#L${obj.getObjectId()}#${obj.getName()}#l`;
            }
        }
        if (content.length > 0)
            cm.sendSimple("#b" + content)
        else {
            cm.sendOk("There are no monsters in the map");
            cm.dispose();
        }
    } else if (status == 2) {
        let obj = player.getMap().getMonsterByOid(selection);
        if (obj != null) {
            let content = "";
            content += `\r\nNAME: ${obj.getName()}`;
            content += `\r\nID: ${obj.getId()}`;
            content += `\r\nOID: ${obj.getObjectId()}`;
            content += `\r\nHP: ${obj.getHp()}`;
            cm.sendOk(content);
            status = 0;
        } else {
            cm.sendOk("The monster could not be found.");
            cm.dispose();
        }
    }
}
features.push(new Selector("List Monsters", ListMonsters));

function StopMonsterControls(selection) {
    if (status == 1) {
        let text = "Your team: " + player.getTeam();
        player.setTeam(-1);
        player.getMap().getMonsters().forEach(m => {
            text += `\r\n#L${m.getObjectId()}#ID: ${m.getId()} \t Team: ${m.getTeam()}#l`;
        });
        cm.sendSimple(text);
    } else if (status == 2) {
        let monster = player.getMap().getMapObject(selection);
        if (monster != null) {
            player.getMap().spawnMesoDrop(10, monster.getPosition(), monster, player, false, 0);
            // player.announce(MaplePacketCreator.killMonster(selection, true));
            // player.announce(MaplePacketCreator.spawnFakeMonster(monster, 0));
            cm.sendNext("Complete!");
        } else {
            cm.sendNext("Monster not found.");
        }
    } else reset();
}
features.push(new Selector("Check Monsters", StopMonsterControls));

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
