const generic = Java.type("com.lucianms.features.GenericEvent");

/* original author: izarooni */
/* modified 8/5/2019 by kerrigan */

let status = 0;
let options = {
    "Spinel": {npc:9000020, script:null},
    "Fun Maps": {npc:1022101, script:null},
    "Training": {npc:9899941, script:null},
    get: function(idx) {
        let current = 0;
        for (let o in options) {
            if (current++ == idx)
              return o;
        }
        return null;
    }
};

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
		let content = "Welcome to the #bChirithy#k #rall-in-one #k#gMap#k NPC! What can I help you with?\r\n#b";
		let i = 0;
		for (let o in options) {
			if (options.hasOwnProperty(o) && typeof options[o] != "function") {
				content += "\r\n#L" + (i++) + "#" + o + "#l";
			}
		}
		cm.sendSimple(content);
    } 
	else if (status == 2) {
        let option = options.get(selection);
        if (option != null) {
			let choice = options[option];
			cm.openNpc((choice.npc == null) ? 2007 : choice.npc, choice.script);
		}
		else {
            cm.dispose();
        }
    }
}
