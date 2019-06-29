const Jobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eHero#k - Uchiha", 112),
    new Job("\r\n\t\t\t\t\t\t", "#ePaladin#k - Rashoumon", 122),
    new Job("\r\n\t\t\t\t\t\t", "#eDark Knight#k - Dragoon", 132),
    new Job("\r\n\t\t\t\t\t   ", "#eDawn Warrior#k - Valkryie\r\n", 1111),
    new Job("\r\n\t\t\t\t", "#eArchmage (Fire/Poison)#k - Cadena", 212),
    new Job("\r\n\t\t\t", "#eArchmage (Ice/Lightning)#k - Luminous", 222),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eBishop#k - Evan\r\n", 232),
    new Job("\r\n\t\t\t\t\t   ", "#eBowmaster#k - PathFinder", 312),
    new Job("\r\n\t\t\t\t\t\t", "#eMarksman#k - Marksman\r\n", 322),
    new Job("\r\n\t\t\t\t\t\t\t", "#eNight Lord#k - Dancer", 412),
    new Job("\r\n\t\t\t\t\t\t", "#eShadower#k - Dual Blader\r\n", 422),
    new Job("\r\n\t\t\t\t\t\t\t", "#eBuccaneer#k - Ark", 512),
    new Job("\r\n\t\t\t\t\t\t   ", "#eCorsair#k - Mechanic\r\n", 522),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eAran#k - Aran", 2112),
];

let status = 0;
function action(mode, type, selection) {
    if (mode < 1) {
        return cm.dispose();
    }
    status++;
    if (status == 1) {
        let content = "\t#dYou view me anytime using the < @jobs > command!\r\n\r\n";
        content += "\t\t\t\t\t\t\t\t\t\t\t #bJob List#k\r\n";
        content += "\t\t\t\tIt costs #b12 rebirth points#k to change jobs\r\n";
        for (let i = 0; i < Jobs.length; i++) {
            let job = Jobs[i];
            content += `${job.format}#L${i}#${job.text}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (player.getRebirthPoints() >= 12) {
            let job = Jobs[selection];
            player.changeJob(Packages.com.lucianms.client.MapleJob.getById(job.jobID));
            player.setRebirthPoints(player.getRebirthPoints() - 12);
            player.sendMessage("You now have {} rebirth points", player.getRebirthPoints());
        } else {
            cm.sendOk("You do not have enough rebirth points");
        }
        cm.dispose();
    }
}

function Job(format, text, jobID) {
    this.format = format;
    this.text = text;
    this.jobID = jobID;
}