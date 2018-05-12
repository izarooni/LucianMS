const MonsterPark = Java.type("com.lucianms.features.MonsterPark");

/* izarooni */
let status = 0;
let options = {
    "Job advancement":     {npc:9201433, script:null},
    "Vote point trader":   {npc:9901754,script:null},
    "Event point trader":  {npc:9901752,script:null},
    "Donor point trader":  {npc:9901755,script:null},
    "Eye Scanner trader":  {npc:9901753,script:null},
    "Monster Coin trader": {npc:2007,   script:"shop_monster_coin"},
    "Overwatch lootbox":   {npc:9270043,script:null},
    "Styler":              {npc:9900001,script:null},
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
            let content = "Welcome to #bLucianMS#k! What can I help you with?\r\n#b";
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
