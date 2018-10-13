/* izarooni */

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
        cm.sendOk("I have no purpose at the moment.");
        cm.dispose();
        // var text = "Hello, I can change you to any job that you'd like\r\nPlease select a job to change to.\r\n#b";
        // var jobs = MapleJob.values();
        // for (var i = 0; i < jobs.length; i++) {
        //     var job = jobs[i];
        //     var name = jobName(job.getId());
        //     if (name != null  && job.getId() >= 0 && job.getId() < 800) {
        //             cjobs.push(job.getId());
        //             text += "\r\n#L" + job.getId() + "#" + name + "#l";
        //     }
        // }
        // cm.sendSimple(text);
    } else {
        // if (cjobs.indexOf(selection) > -1) {
        //     cm.getPlayer().setJob(MapleJob.getById(selection));
        //     cm.getPlayer().updateSingleStat(MapleStat.JOB, selection);
        //     player.setMasteries(selection);
        // }
        // cjobs = null;
        // cm.dispose();
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
