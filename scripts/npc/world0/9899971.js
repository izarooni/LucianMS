/*
 
   Author: Lucasdieswagger @ discord
 
*/

var sections = {};
var method = null;
var status = 0;
var text = "";

var moveTo = 90000007;

function start() {
    action(1, 0, 0);
}

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
        if (cm.getPlayer().getKillType() == 8130101) {
            if (cm.getPlayer().getCurrent() >= cm.getPlayer().getGoal()) {
                // complete quest
                cm.getPlayer().gainExp(1000, 0, true, true, false);
                cm.getPlayer().changeMap(moveTo);
                cm.dispose();
            } else {
                var amountLeft = cm.getPlayer().getGoal() - cm.getPlayer().getCurrent();
                text = "You still need to kill " + amountLeft + " Heartless to continue.";
            }
        } else {
            text = "Lets begin to erase the darkness from your heart, body and mind. Kill 15 #rHeartless#k. You can kill them by standing nearby them and clicking on your attack key (default: #bctrl#k).";
            cm.getPlayer().setKillType(8130101);
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

