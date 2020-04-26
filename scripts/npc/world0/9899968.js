const MapleStat = Java.type('com.lucianms.client.MapleStat');
const Jobs = [
    new Job("\r\n\t\t\t\t\t\t\t", "#eBeginner#k - Beginner", 0),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eHero#k - Uchiha", 112),
    new Job("\r\n\t\t\t\t\t\t", "#ePaladin#k - Rashoumon", 122),
    new Job("\r\n\t\t\t\t\t\t", "#eDark Knight#k - Dragoon", 132),
    new Job("\r\n\t\t\t\t\t   ", "#eDawn Warrior#k - Valkyrie\r\n", 1111),
    new Job("\r\n\t\t\t\t", "#eArchmage (Fire/Poison)#k - Demon Slayer", 212),
    new Job("\r\n\t\t\t", "#eArchmage (Ice/Lightning)#k - Battle Mage", 222),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eBishop#k - Evan\r\n", 232),
    new Job("\r\n\t\t\t\t\t   ", "#eBowmaster#k - PathFinder", 312),
    new Job("\r\n\t\t\t\t\t\t", "#eMarksman#k - Wildhunter\r\n", 322),
    new Job("\r\n\t\t\t\t\t\t\t", "#eNight Lord#k - Phantom", 412),
    new Job("\r\n\t\t\t\t\t\t", "#eShadower#k - Dual Blader\r\n", 422),
    new Job("\r\n\t\t\t\t\t\t\t", "#eBuccaneer#k - Ark", 512),
    new Job("\r\n\t\t\t\t\t\t   ", "#eCorsair#k - Mechanic\r\n", 522),
	new Job("\r\n\t\t\t\t\t\t   ", "#eNight Walker#k - NW\r\n", 1411),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eAran#k - Aran", 2112),
];

let status = 0;
function action(mode, type, selection) {
    if (mode < 1) {
        return cm.dispose();
    }
    status++;

    let cost = player.getRebirths() <= 100 ? 0 : 1;

    if (status == 1) {
        let content = "\tYou view me anytime using the < #d@jobs#k > command!\r\n\r\n";
        if (cost == 0) {
            content += "\t  You may freely change your job for now!\r\n\r\n         #rNOTE!#k You will be changed into 4th job.";
        } else {
            content += `\t\t\t\tIt costs #b${cost} rebirth points#k to change jobs\r\n`;
        }
        for (let i = 0; i < Jobs.length; i++) {
            let job = Jobs[i];
            content += `${job.format}#L${i}#${job.text}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (player.getRebirthPoints() >= cost || player.isDebug()) {
            let job = Jobs[selection];
            player.setJob(Packages.com.lucianms.client.MapleJob.getById(job.jobID));
            player.updateSingleStat(MapleStat.JOB, player.getJob().getId());
            if (cost > 0 && !player.isDebug()) {
                player.setRebirthPoints(player.getRebirthPoints() - cost);
                player.sendMessage("You now have {} rebirth points", player.getRebirthPoints());
            }
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