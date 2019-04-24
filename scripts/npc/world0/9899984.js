/*
 
   Author: Lucasdieswagger @ discord
 
*/

var sections = {};
var method = null;
var status = 0;
var text = "";

var moveTo = 90000004;

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
        if (cm.getPlayer().getKillType() == 9895226) {
            if (cm.getPlayer().getCurrent() >= cm.getPlayer().getGoal()) {
                // complete quest
                cm.getPlayer().gainExp(1000, 0, true, true, false);
                cm.getPlayer().changeMap(moveTo);
                cm.dispose();
            } else {
                var amountLeft = cm.getPlayer().getGoal() - cm.getPlayer().getCurrent();
                text = "You still need to kill " + amountLeft + " Pieces of Wandering Memory to continue.";
            }
        } else {
            text = "You have erased the darkness! Great job. Now you must collect the pieces of the memory you have lost. Kill 10 #rPieces of wandering Memory#k. You can kill them by standing nearby them and clicking on your attack key (default: #bctrl#k).";
            cm.getPlayer().setKillType(9895226);
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
