/* izarooni */
let status = 0;
let MirrorSelections = [
    new Mirror(0, "Lion Kings Castle", 211060010),
	new Mirror(0, "Gate to the future", 271000000),
	new Mirror(0, "Noragami Aragato", 551030800),
	new Mirror(0, "Twilight Perion", 273000000),
	new Mirror(0, "Dark World Tree", 105300100),
	new Mirror(0, "Arcane River", 450001000),
	new Mirror(0, "End of The World", 90000015),

	
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "";
        for (let i = 0; i < MirrorSelections.length; i++) {
            let mirror = MirrorSelections[i];
            if (mirror == undefined) continue;
            content += `#${i}# ${mirror.name}`;
        }
        cm.sendDimensionalMirror(content);
    } else if (status == 2) {
        let mirror = MirrorSelections[selection];
        if (mirror == undefined) {
            cm.sendDimensionalMirror("#-1# Unable to go to that location.");
        } else {
            cm.getPlayer().saveLocation("MIRROR");
            cm.warp(mirror.fieldID, mirror.portalID);
        }
    }
}

function Mirror(levelReq, name, fieldID, portalID) {
    this.levelReq = levelReq;
    this.name = name;
    this.fieldID = fieldID;
    this.portalID = portalID || 0;
}
