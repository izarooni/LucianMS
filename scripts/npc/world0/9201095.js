/* izarooni */

var DungeonBuilder = Java.type("com.lucianms.features.dungeon.DungeonBuilder");

/*
var MapleJob = Java.type("client.MapleJob");
var MapleStat = Java.type("client.MapleStat");

var status = 0;
var cjobs = [];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var text = "Hello, I can change you to any job that you'd like\r\nPlease select a job to change to.\r\n#b";
        var jobs = MapleJob.values();
        for (var i = 0; i < jobs.length; i++) {
            var job = jobs[i];
            var name = jobName(job.getId());
            if (name != null && job.getId() != 800 && job.getId() != 900 && job.getId() != 910) {
                cjobs.push(job.getId());
                text += "\r\n#L" + job.getId() + "#" + name + "#l";
            }
        }
        cm.sendSimple(text);
    } else {
        if (cjobs.indexOf(selection) > -1) {
            cm.getPlayer().setJob(MapleJob.getById(selection));
            cm.getPlayer().updateSingleStat(MapleStat.JOB, selection);
        }
        cjobs = null;
        cm.dispose();
    }
}

function jobName(jobId) {
    var job = cm.getJobName(jobId);
    if (job == null)
        return null;
    else if (jobId == 2001 || jobId >= 2200 || (jobId / 100) == 120)
        return null; // wtf evans
    var ret = "";
    var namesp = job.name().replace("_", " ").split(" ");
    for (var i = 0; i < namesp.length; i++) {
        if (namesp[i].length == 2)
            ret += namesp[i];
        else
            ret += namesp[i].charAt(0) + namesp[i].substring(1).toLowerCase();
        ret += " ";
    }
    return ret.trim();
}
*/

// Temporary file for testing, uncomment above and comment this when done testing (remind me, me.)
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var content = "Starting procedure.";
        cm.sendNext(content);
    } else if (status == 2) {
        // You should probably only let the party leader talk in case there's a party and party play is enabled.
        var dungeon = DungeonBuilder.prepare(cm.getPlayer(), 951000000);
        if(!dungeon
        .setAllowParty(false) // disallow parties
        .setEveryoneNeedsItemRequirements(true) // everyone needs the item requirement
        .setMaxLevel(200) // set the max level for entering the dungeon
        .setMinLevel(60) // set the minimal level for entering the dungeon
        .setScaleEXP(true) // scale EXP to level
        .setScaleFromTotal(10) // if scaleEXP is true, set how much to scale it
        .setTimeLimit(300) // time limit in seconds
        .setReturnMap(820000000) // set return point
        .setMonstersPerPoint(1) // monsters per spawnpoint
        .setDimensions(780, 937, [-24]) // dimensions of the map
        .setMaxMonsterAmount(25) // max amount of monsters in map
        .setRespawnTime(2) // monster respawn time in seconds
        .setDisableDrops(true) // disable drops in map, duhh!
        .attachRequirements(4031544) // ETC items you need to have to go in
        .attachSpawns(100100, 100100, 100100, 100100,100100) // add spawns
        .enter()) { // go into map
            cm.sendOk("Some problems have occurred." + dungeon.getAreLacking()); // give feedback on what went wrong to talker
        }
        cm.dispose();
    }
}
