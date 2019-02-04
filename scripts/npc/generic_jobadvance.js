const MapleStat = Java.type('com.lucianms.client.MapleStat');
const MapleJob = Java.type('com.lucianms.client.MapleJob');
const SkillFactory = Java.type('com.lucianms.client.SkillFactory');
/* izarooni */
let status = 0;

let jobs = {};

function getNpcPath() {
    let id = cm.getNpc() % 9909990;
    if (id == 6) return "power";
    else if (id == 7)  return "darkness";
    else if (id == 8) return "swiftness";
    else if (id == 9) return "magic";
    return undefined;
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let level = player.getLevel();
    let jobID = player.getJob().getId();
    let advancement = (jobID % 100 % 10);

    if (jobID != 0 && jobID != 1000 && jobID != 2000
          && !containsJob(jobs, jobID)) {
        cm.sendOk("This path is not meant for you now.");
        cm.dispose();
        return;
    }

    if (level >= 10 && (jobID == 0 || jobID == 1000 || jobID == 2000)) FirstAdvancement(selection);
    else if (level >= 30 && (jobID % 100 / 10) == 0) SecondAdvancement(selection);
    else if (level >= 70 && advancement == 0) ThirdAdvancement(selection);
    else if (level >= 120 && advancement == 1) FourthAdvancement(selection);
    else {
        cm.sendOk("#bA mysterious force repels you from going near the door");
        cm.dispose();
    }
}

function containsJob(jobs, jobID) {
    for (let adv in jobs) { // advancements
        for (let j in jobs[adv]) { // listed jobs
            if (jobs[adv][j].ID == jobID)
                return true;
        }
    }
    return false;
}

function nextJob(obj, jobID) {
    let dec = (jobID % 100 / 10) == 0 ? 10 : 1;
    for (let o in obj) {
        if (obj[o].ID - dec == jobID)
            return obj[o];
    }
    return null;
}

function getByID(obj, selection) {
    for (var o in obj) {
        if (obj[o].ID == selection)
            return obj[o];
    }
    return null;
}

const FirstAdvancement = function(selection) {
    if (status == 1) {
        cm.sendNext(FirstAdvancementGreet);
    } else if (status == 2) {
        let content = "What path of #b" + getNpcPath() + "#k will you choose?\r\n#b";
        let avail = jobs.First;
        for (let job in avail) {
            if (!(avail[job] instanceof Function))
                content += `\r\n#L${avail[job].ID}#The path of a ${job.toLowerCase()}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 3) {
        let nJob = getByID(jobs.First, selection);
        if (nJob != null) {
            if (nJob.req(player)) {
                let give = nJob.equips;
                for (let i = 0; i < give.length; i++) {
                    cm.gainItem(give[i][0], give[i][1]);
                }
                player.changeJob(MapleJob.getById(selection));
                LearnSkills(nJob.skills);
                let totalSP = 1 + (Math.min(30, player.getLevel()) - 10) * 3;
                if (totalSP > 0) {
                    player.setRemainingSp(totalSP);
                    player.updateSingleStat(MapleStat.AVAILABLESP, totalSP);
                }
            } else
                cm.sendOk("You are not strong enough to make this advancement.\r\n" + nJob.failMessage);
        } else
            cm.sendOk("That is not allowed.");
        cm.dispose();
    }
};

const SecondAdvancement = function(selection) {
    if (status == 1) {
        let content = "You have become strong but a long journey still awaits.\r\nAre you ready to progress?\r\n#b";
        let oLength = content.length;
        for (let job in jobs.Second) {
            let nj = jobs.Second[job];
            if (Math.floor(nj.ID / 100) == player.getJob().getId() / 100) {
                content += `\r\n#L${nj.ID}#${job}#l`;
            }
        }
        if (oLength == content.length){
            cm.sendOk("For some reason you apparently don't have any jobs to advance to");
            cm.dispose();
        } else
            cm.sendSimple(content);
    } else if (status == 2) {
        let nJob = getByID(jobs.Second, selection);
        player.changeJob(MapleJob.getById(nJob.ID));
        LearnSkills(nJob.skills);
        cm.dispose();
    }
};

const ThirdAdvancement = function(selection) {
    if (status == 1) {
        let content = "From here on you will only grow stronger.\r\nAre you ready to progress?";
        cm.sendNext(content);
    } else if (status == 2) {
        let nJob = nextJob(jobs.Third, player.getJob().getId());
        player.changeJob(MapleJob.getById(nJob.ID));
        LearnSkills(nJob.skills);
        cm.dispose();
    }
};

const FourthAdvancement = function(selection) {
    if (player.getJob().getId() / 100 > 11 && player.getJob() < 2000) {
        cm.dispose();
    } else if (status == 1) {
        cm.sendNext("The end is near and you'll be the one to defeat it....right?");
    } else if (status == 2) {
        let nJob = nextJob(jobs.Fourth, player.getJob().getId());
        if (nJob != null) {
            player.changeJob(MapleJob.getById(nJob.ID));
            LearnSkills(nJob.skills);
        }
        cm.dispose();
    }
    // if (status == 1)
    //     cm.sendSimple("#bThe mysterious force emitted by the door is weakened...\r\n#b#L0#Enter#l\r\n#L1#Nevermind#l", 2)
    // else if (status == 2) {
    //     if (selection == 0)
    //         cm.warp(551030804); // boss ID: 9895226
    //     cm.dispose();
    // }
};

const LearnSkills = function(skills) {
    if (skills == null) return;
    for (let i = 0; i < skills.length; i++) {
        let skillID = skills[i][0];
        let maxLevel = skills[i][1];
        let skill = SkillFactory.getSkill(skillID);
        if (skill != null) {
            player.changeSkillLevel(skill, 0, maxLevel, -1);
        }
    }
    // if (status == 1)
    //     cm.sendSimple("#bThe mysterious force emitted by the door is weakened...\r\n#b#L0#Enter#l\r\n#L1#Nevermind#l", 2)
    // else if (status == 2) {
    //     if (selection == 0)
    //         cm.warp(551030804); // boss ID: 9895226
    //     cm.dispose();
    // }
};