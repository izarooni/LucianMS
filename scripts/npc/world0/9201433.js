/* Lucas */
var MapleJob = Java.type("client.MapleJob");
var MapleStat = Java.type("client.MapleStat");

var levelRequirement = 0;

var status = 0;
var cjobs = [];

function action(mode, type, selection) {
    var els = "";
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var jobId = cm.getPlayer().getJob().getId();
        if(jobId >= 0 && jobId <= 600) {
            // explorer
            els = prepareJobSpecifics(0, jobId, 100, 600);
        } else if(jobId >= 1000 && jobId <= 1600) {
            // cygnus
            els = prepareJobSpecifics(1000, jobId, 1100, 1600);
        } else if(jobId >= 2000 && jobId < 2200 && jobId != 2001) {
            // aran
            els = prepareJobSpecifics(2000, jobId, 2100, 2200);
        } else {
            cjobs = []; // lol we aren't going to allow evans.
        }

        if(!cjobs.length == 0) {
            cm.sendSimple(els);
        } else {
            cm.sendOk("          You currently cannot advance to a new job.");
        }

    } else {
        if (cjobs.indexOf(selection) > -1) {
            if(cm.getPlayer().getLevel() >= levelRequirement) {
                cm.getPlayer().setJob(MapleJob.getById(selection));
                cm.getPlayer().updateSingleStat(MapleStat.JOB, selection);
            } else {
                cm.sendOk("          You need to be atleast at level " + levelRequirement + " to advance your job, try training harder!"); // they shouldn't actually come here, but just in case lol.
            }
        }
        cjobs = null;
        cm.dispose();
    }
}

function prepareJobSpecifics(lowest, jobId, jobMin, jobMax) {
    var els = "          Hello Lucian hero, are you ready for your\r\n          #bjob advancement#k?";
    var jobs = MapleJob.values();
    if(jobId != lowest) {
        var addon = (jobId / 100 - Math.floor(jobId / 100));
        if(addon === 0) {
            addon = 10;
            levelRequirement = 30;
        } else {
            addon = 1;
            if(jobName(jobId+2) != null) {
                levelRequirement = 70;
            } else {
                levelRequirement = 120;
            }
        }
        for(var i = 0; i < jobs.length; i++) {
            var job = jobs[i];
            var name = jobName(job.getId());
            if(job.getId() == jobId+addon && name != null && (addon == 1 && cjobs.length == 0 || addon == 10)) {
                if(cm.getPlayer().getLevel() >= levelRequirement) {
                    els += "\r\n          #L" + job.getId() + "#" + name + "#l";
                    jobId += (addon);
                    cjobs.push(job.getId());
                }
            }
        }
    } else {
        if(cm.getPlayer().getLevel() >= 10) {
            for(var i = jobMin; i < jobMax; i+=100) {
                var name = jobName(i);
                if(name != null) {
                    els += "\r\n          #L" + i + "#" + name + "#l";
                    cjobs.push(i);
                }
            }
        }
    }

    return els;
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
