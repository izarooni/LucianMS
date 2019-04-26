/* izarooni */
let Locations = [
    new Location(105300100, {min: 0, max:150}),
    new Location(273000000, {min: 150, max: 1000}),
    new Location(211060010, {min: 1000, max: 10000}),
    new Location(551030800, {min: 10000, max: Number.MAX_VALUE})
];
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let destination = undefined;
        let rbs = player.getRebirths();
        for (let i = 0; i < Locations.length; i++) {
            let location = Locations[i];
            let req = location.rebirths;
            if (rbs >= req.min && rbs < req.max) {
                destination = location.fieldID;
                break;
            }
        }
        cm.vars = { location: destination };
        cm.sendNext("If you'd like the easy way to build up your strength then you are in the right place."
        + `\r\nYou have #b${player.getRebirths()} rebirths#k, so I would recommend:`
        + `\r\n#b#m${destination}##k`
        + "\r\nWhat do you say, do you want to go?");
    } else if (status == 2) {
        let destination = cm.vars.location;
        if (destination != undefined) {
            cm.warp(destination);
        }
        cm.dispose();
    }
}
function Location(fieldID, rebirths) {
    this.fieldID = fieldID;
    this.rebirths = rebirths;
}
/*
Easy: Damien world tree - 105300100 / 0 - 150 rbs
Normal: Twilight Perion - 273000000 / 150 - 1, 000 rbs
Medium: LHC castle - 211060010 / 1, 000 - 10, 000 rbs
Hard: Noragami - 551030800 / 10, 000 and above
*/