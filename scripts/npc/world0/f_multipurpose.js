const MonsterPark = Java.type("com.lucianms.features.MonsterPark");

/* izarooni */

let status = 0;
let options = {
    "Vote Point Trader":   {npc:9901754,    script:null},
    "Donor Point Trader":   {npc:9901755, script:null},
    "Styler":              {npc:9900001,    script:null},
	"Monster Park Shuttle": {npc:9071000, 	script:9071003},
    "Job Advancement": {npc: 9900000, script:9900000},

    get: function(idx) {
        let cidx = 0;
        for (let o in options) {
            if (cidx++ == idx) return o;
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
        if (this.optional.isPresent()) {
            cm.openNpc(9071000, "f_monster_park_quit");
        } else {
            let content = "Welcome to the #bChirithy#k navigation panel. What would you like to do?\r\n#d";
            let i = 0;
            for (let o in options) {
                if (options.hasOwnProperty(o) && typeof options[o] != "function") {
                    content += "\r\n#L" + (i++) + "#" + o + "#l";
                }
            }
            cm.sendSimple(content);
        }
    } else if (status == 2) {
        let option = options.get(selection);
        if (option != null) {
            let obj = options[option];

            /*
            1: if player npc, null the npc ID
            2: if true, use npc ID as script name instead and
            use Maple Administrator as NPC speaker to prevent
            user crashing from attempting to view non-existing
            player NPC object
            */
            let npc = (obj.npc >= 9901000) ? null: obj.npc;
            let script = (npc == null) ? obj.npc + "" : obj.script;
            cm.openNpc((npc == null) ? 2007 : npc, script);
        } else {
            cm.dispose();
        }
    }
}
