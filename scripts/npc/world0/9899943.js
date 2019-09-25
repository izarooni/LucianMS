 /*
 
	Author: Lucasdieswagger @ discord
 
 */
 
 var sections = {};
 var method = null;
 var status = 0;
 var text = "";

 
function start() {
	action(1, 0, 0);
}


sections["Take the Mark of Mastery exam"] = function(mode, type, selection) {
	if(status === 2) {
		cm.sendYesNo("Are you entirely sure you wish to take the exam?\r\n#rYou will lose exp and skills that you have accumulated during this adventure, and you have a 30% chance to fail.");
	} else if(status === 3) {
		var random = Math.random() * 100 + 1;
		if(random >= 30) {
			cm.getPlayer().doRebirth() 
			cm.getPlayer().dropMessage(6, "You feel a strange power draining your age, turning you back to where you began.");
		} else {
			cm.getPlayer().dropMessage(5, "You forcefully try breaking through, but you fail and hurt yourself.");
			cm.getPlayer().setHpMp(25);
		}
		cm.dispose();
	}
}

sections["I do not feel ready."] = function(mode, type, selection) {
	cm.dispose();
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
		if(!(cm.getPlayer().getLevel() >= 200)) {
			text = "You are not worthy to pass the exam!";
			cm.sendOk(text);
		} else {
			text = "You look strong enough, do you want to attempt to pass the #kMark of Mastery#k exam?";
        var i = 0;
        for (var s in sections) {
            text += "\r\n#b#L" + (i++) + "#" + s + "#l#k";
        }
        cm.sendSimple(text);
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

