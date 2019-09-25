/* izarooni */
let status = 0;
let MirrorSelections = [
    new Mirror(0, "Ariant Coliseum", 980010000, 3),
    new Mirror(0, "Mu Lung Dojo", 925020000),
    new Mirror(0, "Monster Carnival 1", 980000000),
    new Mirror(0, "Monster Carnival 2", 980030000),
    undefined,
    new Mirror(0, "Construction Site", 910320000),
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