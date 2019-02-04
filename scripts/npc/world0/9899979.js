 /*
 
    Author: Lucasdieswagger @ discord
 
 */
 importPackage(Packages.tools);
var LifeFactory = Java.type("com.lucianms.server.life.MapleLifeFactory");
 var sections = {};
 var method = null;
 var status = 0;
 var text = "";

 var moveTo = 122000006;
 
function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
        return;
    } else if (mode === 0) {
        status--;
        if (status === 0) {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status === 1) {
        method = null;
        if(cm.getPlayer().getKillType() == 100124) {
            if(cm.getPlayer().getCurrent() >= cm.getPlayer().getGoal()) {
                // complete quest
                cm.getPlayer().gainExp(1000, 0, true, true, false);
                cm.getPlayer().changeMap(moveTo);
                cm.dispose();
                cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear4"))
                 cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound("customJQ/quest"));
            } else {
                var amountLeft = cm.getPlayer().getGoal() - cm.getPlayer().getCurrent();
                text = "You still need to kill " + amountLeft + " monsters to continue.";
            }
        } else {
            text = "So you wanna take part in playing hero with us? Great!. Let's start. There are some monsters running around in our beautiful forest. You can kill them by standing nearby them and clicking on your attack key (default: #bctrl#k), kill 10 Tiguru's and talk to me again.";
            cm.getPlayer().setKillType(100124);
            cm.getPlayer().setGoal(10);
            cm.getPlayer().setCurrent(0);
        }
        cm.sendOk(text);
        cm.dispose();
        
    } else {
        if (method == null) {
            method = sections[get(selection)];
        }
        method(mode, type, selection);
    }
}


function get(index) {
    var i = 0;
    for (var s in sections) {
        if (i === index)
            return s;
        i++;
    }
    return null;
}

