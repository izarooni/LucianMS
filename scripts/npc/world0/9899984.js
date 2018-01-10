/*
 
    Author: Lucasdieswagger @ discord
 
 */
 

 importPackage(Packages.tools);
var LifeFactory = Java.type("server.life.MapleLifeFactory");
 var sections = {};
 var method = null;
 var status = 0;
 var text = "";

 var moveTo = 3;
 
function start() {
    action(1, 0, 0);
}

sections["Go to the Home Map"] = function(mode, type, selection) {
    if(status >= 1) {
        while(cm.getPlayer().getLevel < 8) {
            cm.getPlayer().gainExp(1500, 0, true, true, false);
        }
        cm.getPlayer().changeMap(809);
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear1"));
        cm.getPlayer().dropMessage(6, "Welcome to LucianMS!");
        cm.getPlayer().getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "Welcome " + cm.getPlayer().getName() + " to LucianMS, let's give him a warm welcome."));
    }
};

sections["Go to Henesys"] = function(mode, type, selection) {
    if(status >= 1) {
        while(cm.getPlayer().getLevel < 8) {
            cm.getPlayer().gainExp(800, 0, true, true, false);
        }
        cm.getPlayer().changeMap(100000000);
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear1"));
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound("customJQ/quest"));
        cm.getPlayer().dropMessage(6, "Welcome to LucianMS!");
        cm.getPlayer().getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "Welcome " + cm.getPlayer().getName() + " to LucianMS, let's give him a warm welcome."));
    }
};

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
        if(cm.getPlayer().getKillType() == 9700068) {
            cm.getPlayer().setCurrent(1);
            if(cm.getPlayer().getCurrent() >= cm.getPlayer().getGoal()) {
                // complete quest
                text = "You are done with the tutorial, where would you like to go now?";
                var i = 0;
                 for (var s in sections) {
                    text += "\r\n#b#L" + (i++) + "#" + s + "#l#k";
                }
                cm.sendSimple(text);
            } else {
                var amountLeft = cm.getPlayer().getGoal() - cm.getPlayer().getCurrent();
                text = "You still need to kill " + amountLeft + " monsters to continue.";
                cm.sendOk(text);
            }
        } else {
            text = "Lets see you kill some monsters, you can kill them by standing nearby them and clicking on your attack key (default: #bctrl#k), kill 10 #o9700068#'s and talk to me again.";
            cm.getPlayer().setKillType(9700068);
            cm.getPlayer().setGoal(1);
            cm.getPlayer().setCurrent(0);
            cm.sendOk(text);
            cm.dispose();
            cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear1"));
        }   
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

