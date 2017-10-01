var InventoryModifier = Java.type(Packages.server.MapleInventoryManipulator);
var status;
var item = [id, id],
var rand = Math.floor(Math.random() * item.length);

var status = 0;
function start() {
    status = -1;
    action (1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.sendOk("Goodbye!");
        cm.dispose();
        return;
    } else if (mode == 0) {
        status--;
    } else {
        status++;
    }
    
    if (status == 0) {
        cm.sendYesNo("Are you sure you want to spin the wheel? /r/n /r/n #e What can I win? #k /r/n #g Monster Coin #v4011020# #k /r/n #r item");
    } else if (status == 1) {
        if (cm.getMeso() >= 1000000) {
            cm.getMeso(-1000000);
            cm.gainItem(item[rand]);
            cm.sendOk("Congrats on your item!");
            cm.dispose();
        }
    }
}