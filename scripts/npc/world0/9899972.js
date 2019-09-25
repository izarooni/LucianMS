/* original by izarooni */
/* remade by kerrigan */
/* jshint esversion: 6 */

let status = 0;

let options = {
    "#kI would like to create an MSI with #drebirth points#k": {npc:9899972, script:"msi_rb"},
    "I would like to create an MSI with #bChirithy coin#k": {npc:9899972, script:"msi_coin"},
    "I would like to create an MSI with rare monster drops": {npc:9899972, script:"msi_drop"},
    get: function(idx) {
        let cidx = 0;
        for (let o in options) {
            if (cidx++ == idx)
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
        this.optional = player.getGenericEvents().stream().filter(function(e){
            return (e instanceof MonsterPark);
        }).findFirst();
        let content = "Welcome to #bChirithy#k! What can I help you with?\r\n#b";
        let i = 0;
        for (let o in options) {
            if (options.hasOwnProperty(o) && typeof options[o] != "function") {
                content += "\r\n#L" + (i++) + "#" + o + "#l";
            }
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let option = options.get(selection);
        if (option != null) {
            let obj = options[option];
            let npc = (obj.npc >= 9901000) ? null: obj.npc;
            let script = (npc == null) ? obj.npc + "" : obj.script;
            cm.openNpc((npc == null) ? 9899972 : npc, script);
        } else {
            cm.dispose();
        }
    }
}
