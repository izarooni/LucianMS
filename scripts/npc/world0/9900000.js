const MapleStat = Java.type('com.lucianms.client.MapleStat');
/*const Jobs = [
    new Job("\r\n\t\t\t\t\t\t\t", "#eBeginner#k - Beginner", 0),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eHero#k - Uchiha", 112),
    new Job("\r\n\t\t\t\t\t\t", "#ePaladin#k - Rashoumon", 122),
    new Job("\r\n\t\t\t\t\t\t", "#eDark Knight#k - Dragoon", 132),
    new Job("\r\n\t\t\t\t\t   ", "#eDawn Warrior#k - Valkyrie\r\n", 1111),
    new Job("\r\n\t\t\t\t", "#eArchmage (Fire/Poison)#k - Cadena", 212),
    new Job("\r\n\t\t\t", "#eArchmage (Ice/Lightning)#k - Luminous", 222),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eBishop#k - Evan\r\n", 232),
    new Job("\r\n\t\t\t\t\t   ", "#eBowmaster#k - PathFinder", 312),
    new Job("\r\n\t\t\t\t\t\t", "#eMarksman#k - Wildhunter\r\n", 322),
    new Job("\r\n\t\t\t\t\t\t\t", "#eNight Lord#k - Dancer", 412),
    new Job("\r\n\t\t\t\t\t\t", "#eShadower#k - Dual Blader\r\n", 422),
    new Job("\r\n\t\t\t\t\t\t\t", "#eBuccaneer#k - Ark", 512),
    new Job("\r\n\t\t\t\t\t\t   ", "#eCorsair#k - Mechanic\r\n", 522),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eAran#k - Aran", 2112),
];
*/

const Jobs = [
    new Job("\r\n\t\t\t\t\t   ", "#eDawnWarrior / Valkyrie#k\r\n", 1100),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eWarrior#k", 100),
    new Job("\r\n\t\t\t\t\t\t", "#eMage#k", 200),
    new Job("\r\n\t\t\t\t\t\t", "#eBowman#k", 300),
    new Job("\r\n\t\t\t\t\t\t", "#eThief#k\r\n", 400),
    new Job("\r\n\t\t\t\t\t\t\t", "#ePirate#k", 500),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eAran#k - Aran", 2000),
];

const WarrJobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eFighter / Uchiha#k", 110),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#ePage / Rashoumon#k", 120),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eSpearman / Dragoon#k", 130),
];

const MageJobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eF/P Wizard / Demon Slayer#k", 210),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eI/L Wizard / Battle Mage#k", 220),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eCleric / Evan#k", 230),
];

const BowJobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eHunter / PathFinder#k", 310),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eCrossbowman / Marksman#k", 320),
];

const ThiefJobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eAssassin / Phantom#k", 410),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eShadower / Dual Blader#k", 420),
];

const PirateJobs = [
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eBuccaneer / Ark#k", 510),
    new Job("\r\n\t\t\t\t\t\t\t\t", "#eCorsair / Mechanic#k", 520),
];

let status = 0;
let newJob = [];


function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    //let cost = player.getRebirths() <= 100 ? 0 : 1;
    let cost = 0;

    if (status == 1) {

        if (player.getLevel() < 30 && (player.getJob().getId() == 0 || player.getJob().getId() == 1000 || player.getJob().getId() == 2000)) {
            newJob = Jobs.slice(0);
            let content = "\tWelcome to choosing your first path!\r\n\r\n";
            if (cost == 0) {
                content += "\t   Choose wisely!\r\n";
            } else {
                //content += `\t\t\t\tIt costs #b${cost} rebirth points#k to change jobs\r\n`;
            }
            for (let i = 0; i < Jobs.length; i++) {
                let job = Jobs[i];
                content += `${job.format}#L${i}#${job.text}#l`;
            }
            cm.sendSimple(content);
        } else if (player.getLevel() >= 30) {
            let currJob = player.getJob().getId() / 100;
            let text = true;
            switch (currJob) {
                case 1:
                    newJob = WarrJobs.slice(0);
                    break;
                case 2:
                    newJob = MageJobs.slice(0);
                    break;
                case 3:
                    newJob = BowJobs.slice(0);
                    break;
                case 4:
                    newJob = ThiefJobs.slice(0);
                    break;
                case 5:
                    newJob = PirateJobs.slice(0);
                    break;
                case 11:
                    player.setJob(Packages.com.lucianms.client.MapleJob.getById(1110));
                    player.updateSingleStat(MapleStat.JOB, 1110);
                    text = false;
                    cm.dispose();
                    break;
                case 20:
                    player.setJob(Packages.com.lucianms.client.MapleJob.getById(2100));
                    player.updateSingleStat(MapleStat.JOB, 2100);
                    text = false;
                    cm.dispose();
                    break;
                default:
                    text = false;
                    cm.sendOk("You don't have a branch or you've already advanced");
                    cm.dispose();
            }

            if (text) {
                let content = "Choose your branch";
                for (let i = 0; i < newJob.length; i++) {
                    let job = newJob[i];
                    content += `${job.format}#L${i}#${job.text}#l`;
                }
                cm.sendSimple(content);
            }
        } else {
            cm.sendOk("You already have chosen a job or you have an unsupported job.");
            cm.dispose();
        }

    }else if (status == 2) {
            if (player.getRebirthPoints() >= cost || player.isDebug()) {

                let job = newJob[selection];

                /*
                switch(job.jobID/100){
                    case 1:

                        //cm.createItemWithStatsAndUpgradeSlots(1112400,1,1,6);
                        break;
                }
                */
                if(player.getLevel() < 30) {
                    cm.gainMeso(30000);
                    cm.gainItem(2002023, 100);
                }
                //player.changeJob(job.jobID);
                player.setJob(Packages.com.lucianms.client.MapleJob.getById(job.jobID));
                player.updateSingleStat(MapleStat.JOB, player.getJob().getId());
                if (cost > 0 && !player.isDebug()) {
                    //player.setRebirthPoints(player.getRebirthPoints() - cost);
                    //player.sendMessage("You now have {} rebirth points", player.getRebirthPoints());
                }
                //cm.sendOk("done");
            } else {
                cm.sendOk("Something happened. Let an admin know.");
            }
            cm.dispose();
        }

}

function Job(format, text, jobID) {
    this.format = format;
    this.text = text;
    this.jobID = jobID;
}