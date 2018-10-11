const CQuests = Java.type("com.lucianms.cquest.CQuestBuilder");
load("scripts/util_cquests.js")
const Quests = {
    "Junior Bosses": {
        Display: true, // to display in the first status (quest category selection)
        IDs: [205, 206, 207, 208, 209, 210, 211, 212, 213], // the quests that belong in this section
        Completed: [], /// quests from this section that can be returned
        Locked: [], // quests from this section that are locked (i.e. needs a pre-quest to complete)
        Metadata: [] // quests that can be accepted
    }
}
/* izarooni
This was coded at 10 AM (no sleep since midday the day before) pls bare with me on this monstrosity
 */
let status = 0;
let selected = {};
// `selected.selection` is used for `selection` persistence when display the quest summary
// `selected.sub` is used to transfer the quest category over multiple statuses
// `selected.quest` is used to transfer the quest selection over multiple statuses

ProcessQuests();

function Reset() {
    status = 0;
    selected = {};

    for (let s in Quests) {
        let sub = Quests[s];
        if (!sub.Display) continue;
        sub.Completed = [];
        sub.Locked = [];
        sub.Metadata = [];
    }

    ProcessQuests();
}

// obtain separate arrays on
// Locked    - Player meets quest level requirement but has yet to complete the pre-quests
// Completed - Player hasn't RETURNED the quest but meets requirements to do so
// Metadata  - Player hasn't accepted the quest but is allowed
function ProcessQuests() {
    for (let q in Quests) { // iterate childs of `Quests`
        let sQuest = Quests[q];
        if (!sQuest.Display) continue;
        let arr = sQuest.IDs;
        for (let i = 0; i < arr.length; i++) {
            let md = CQuests.getMetaData(arr[i]);
            if (md != null && player.getLevel() >= md.getMinimumLevel()) {
                let pqid = md.getPreQuestId();
                if (!CanAcceptQuest(md)) { // quest is locked
                    sQuest.Locked.push(md);
                } else if (player.getCustomQuest(arr[i]) != null) { // quest is completed but not returned
                    let pquest = player.getCustomQuest(arr[i]);
                    if (!pquest.isCompleted() && pquest.checkRequirements()) {
                        sQuest.Completed.push(pquest);
                    }
                } else { // quest can be accepted
                    sQuest.Metadata.push(md);
                }
            }
        }
    }
}

// check if the quest exists and is RETURNED
function IsQuestCompleted(preQuestID) {
    let preQuest = player.getCustomQuest(preQuestID);
    return preQuest != null && preQuest.isCompleted();
}

// check if the player has completed the pre-quests to
// enable acceptance of a specified quest
function CanAcceptQuest(meta) {
    let pqid = meta.getPreQuestId();
    if (pqid > 0) {
        if (!IsQuestCompleted(pqid)) {
            return false; // metadata of prequest
        }
    } else if (pqid == -2) {
        let pqids = meta.getPreQuestIDs(); // metadata of multiple prequests
        for (let i = 0; i < pqids.length; i++) {
            if (!IsQuestCompleted(pqids[i])) {
                return false;
            }
        }
    }
    return true;
}

// returns an array of pre-quest metadata of
// the specified quest
function getRequiredQuests(meta) {
    let ret = [];
    let pqid = meta.getPreQuestId();
    if (pqid > 0) {
        if (!IsQuestCompleted(pqid)) {
            ret.push(CQuests.getMetaData(pqid));
        }
    } else {
        let pqids = meta.getPreQuestIDs();
        for (let i = 0; i < pqids.length; i++) {
            if (!IsQuestCompleted(pqids[i])) {
                ret.push(CQuests.getMetaData(pqids[i]));
            }
        }
    }
    return ret;
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "What kind of bosses are you in the mood to slay today?\r\n#b";
        let i = 0;
        for (let q in Quests) {
            if (!Quests[q].Display) continue;
            content += `\r\n#L${i}#${q}#l`;
            i++;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let content = "";
        let i = 0;
        for (let q in Quests) {
            if (!Quests[q].Display) continue;
            if (i != selection) {
                i++;
                continue;
            }
            selected.sub = Quests[q];
            let arr      = selected.sub.Metadata;
            let complete = selected.sub.Completed;
            let locked   = selected.sub.Locked;
            
            if (locked.length == 0 && arr.length == 0) {
                cm.sendOk("You do not have any Boss Hunter quests available right now. Come back when you have become stronger.");
                cm.dispose();
                return;
            }
            let uid = 0;

            if (arr.length > 0) {
                content += `${AvailableIcon}#b`;
            }
            for (let i = 0; i < arr.length; i++) {
                let metadata = arr[i];
                content += `\r\n#L${uid++}#Lvl ${metadata.getMinimumLevel()} - ${metadata.getName()}#l`;
            }

            if (complete.length > 0) {
                content += `${CanCompleteIcon}#b`;
            }
            for (let i = 0; i < complete.length; i++) {
                let quest = complete[i];
                content += `\r\n#L${uid++}#Lvl ${quest.getMinimumLevel()} - ${quest.getName()}#l`;
            }

            content += "\r\n#k";
            for (let i = 0; i < locked.length; i++) {
                let metadata = locked[i];
                content += `\r\n#L${uid++}#${LockedIcon} Lvl ${metadata.getMinimumLevel()} - ${metadata.getName()}#l`;
            }
            break;
        }
        cm.sendSimple(content);
    } else if (status == 3) {
        if (selected.selection == undefined) selected.selection = selection;
        else if (selection == -1) selection = selected.selection;
        selection = selected.selection;

        let sMetadata = selected.sub.Metadata;
        let offset = sMetadata.length + selected.sub.Completed.length;

        if (selection >= sMetadata.length && selection < offset) {
            selected.quest = selected.sub.Completed[selection - sMetadata.length];
            status = 5;
            action(1, 0 -1);
        } else if (selection < sMetadata.length) {
            let metadata = (selected.quest = sMetadata[selection]);
            DisplaySummary(metadata);
        } else {
            let content = `You must complete the following quests before you may accept this\r\n#b`;
            let rquests = getRequiredQuests(selected.sub.Locked[selection - offset]);
            for (let i = 0; i < rquests.length; i++) {
                let rquest = rquests[i];
                content += `\r\n${rquest.getName()}`;
            }
            cm.sendNext(content);
            selected = {};
            status = 0;
        }
    } else if (status == 4) {
        cm.sendAcceptDecline("I know this is easy for you... Accept this task it'll be no problem! You need to fight the strong to become strong and what better way to do it?");
    } else if (status == 5) {
        CQuests.beginQuest(player, selected.quest.getQuestId());
        cm.dispose();
    } else if (status == 6) {
        let pquest = selected.quest;
        if (pquest.checkRequirements()) {
            if (pquest.complete(player)) {
                cm.sendNext("Easy as cake! I knew you could do it. I've got more tasks if you're willing to do more work for me");
            } else {
                cm.sendOk("You must make room in your inventory to accept my rewards");
                cm.dispose();
            }
        }
        Reset();
    }
}