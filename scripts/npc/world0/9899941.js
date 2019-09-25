const ForcedStat = Java.type('com.lucianms.client.meta.ForcedStat');
/* izarooni */
let Locations = [
    new Location(100040001, {min: 0, max: 2000}),
    new Location(103000101, {min: 2001, max: 5000}),
    new Location(105040306, {min: 5001, max: 15000}),
    new Location(541000300, {min: 15001, max: 35000}),
    new Location(240070200, {min: 35001, max: 60000}),
    new Location(600020300, {min: 60001, max: 100000}),
    new Location(800020130, {min: 100001, max: 150000}),
    new Location(240040511, {min: 150001, max: 250000}),
    new Location(211060010, {min: 250001, max: 750000}),
    new Location(271000000, {min: 750001, max: 1500000}),
    new Location(273000000, {min: 1500001, max: 3000000}),
    new Location(541020000, {min: 3000001, max: 5000000}),
    new Location(105300100, {min: 5000001, max: Number.MAX_VALUE})
];
let status = 0;
let player_range = Math.max(player.calculateMaxBaseDamage(player.getTotalWatk()), player.getTotalMagic());

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let destination = undefined;
        for (let i = 0; i < Locations.length; i++) {
            let location = Locations[i];
            let req = location.range;
            if (player_range >= req.min && player_range < req.max) {
                destination = location.fieldID;
                break;
            }
        }
        cm.vars = { location: destination };
        cm.sendYesNo("Hi. I'm your personal trainer. I can give you training map suggestions depending on your current range."
            + `\r\nYour current maximum range is #d${Math.max(player.calculateMaxBaseDamage(player.getTotalWatk()), player.getTotalMagic())}#k, so I would recommend:\r\n`
            + `\r\n#d#m${destination}##k\r\n`
            + "\r\nDo you want to warp there now?");
    } else if (status == 2) {
        let destination = cm.vars.location;
        if (destination != undefined) {
            cm.warp(destination);
           /* let fc = new ForcedStat();
            fc.setBonusExpRate(8);
            player.setForcedStat(fc);*/
        }
        cm.dispose();
    }
}
function Location(fieldID, range) {
    this.fieldID = fieldID;
    this.range = range;
}
/*
Easy: Damien world tree - 105300100 / 0 - 150 rbs
Normal: Twilight Perion - 273000000 / 150 - 1, 000 rbs
Medium: LHC castle - 211060010 / 1, 000 - 10, 000 rbs
Hard: Noragami - 551030800 / 10, 000 and above
*/