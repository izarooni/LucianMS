const Achievements = Java.type('com.lucianms.io.scripting.Achievements');
const AStatus = Java.type('com.lucianms.client.meta.Achievement').Status;
/* izarooni */
let status = 0;
let achievements = {
    incomplete: [],
    completed: [],
    rewarded: [],
};

player.getAchievements().entrySet().forEach(function (e) {
    let name = e.getKey();
    let achieve = e.getValue();
    switch (achieve.getStatus().ordinal()) {
        case 0: return achievements.incomplete.push(new Achievement(name, achieve));
        case 1: return achievements.completed.push(new Achievement(name, achieve));
        case 2: return achievements.rewarded.push(new Achievement(name, achieve));
    }
});

function action(mode, type, selection) {
    if (mode < 1) {
        status--;
        if (status == 0) {
            return cm.dispose();
        }
    } else {
        status++;
    }
    if (status == 1) {
        cm.vars = { section: undefined };
        let completedCount = achievements.completed.length + achievements.rewarded.length;
        let totalCount = player.getAchievements().size();
        let text = `You have completed #b ${completedCount} / ${totalCount} achievements\r\n#b`
            + "\r\n#L0#Completed Achievements#l"
            + "\r\n#L1#Incomplete Achievements#l";
        if (achievements.completed.length > 0) {
            text += "\r\n#L2#Claim rewards#l"
        }
        cm.sendSimple(text, 2);
    } else if (status == 2) {
        let text = "#b\r\n";
        let section = cm.vars.section;
        if (section == undefined) {
            if (selection == 0) section = achievements.rewarded; // complete (rewards given)
            else if (selection == 1) section = achievements.incomplete; // incomplete
            else if (selection == 2) section = achievements.completed; // complete (rewards available)
            cm.vars.section = section;
        }

        if (section == undefined) {
            cm.sendOk("#bHello from the developer!#k\r\n\t\t- izarooni");
            return cm.dispose();
        } else if (section.length == 0) {
            cm.sendNext("There is nothing here\r\n");
            status = 0;
            return;
        }
        let prevLength = text.length;
        for (let i = 0; i < section.length; i++) {
            let a = section[i];
            if (a == undefined) continue;
            text += `\r\n#L${i}##FUI/UIWindow/Memo/check${a.obj.getStatus().ordinal() == 2 ? 1 : 0}# ${a.name}#l`;
        }
        if (prevLength < text.length) cm.sendSimple(text, 2);
        else {
            status = 0;
            action(1, 0, 0);
        }
    } else if (status == 3) {
        let section = cm.vars.section;
        if (section == undefined) {
            cm.sendOk("#bHello from the developer!#k\r\n\t\t- izarooni");
            return cm.dispose();
        }
        let achieve = section[selection];
        if (player.isDebug()) {
            if (player.getAchievements().remove(achieve.name)) {
                cm.sendOk("Achievement removed");
                return cm.dispose();
            }
        }
        let naStatus = achieve.obj.getStatus().ordinal();
        let description = achieve.obj.getDescription(player);
        let content = "";
        if (description != null) content += "#e" + description + "#n\r\n";
        content += `\r\nHere are the rewards for completing the\r\n'#b${achieve.name}#k' achievement\r\n#b`;
        let rewards = Achievements.getRewards(achieve.name);
        if (!rewards.isEmpty()) {
            for (let reward in rewards) {
                content += `\r\n${rewards[reward]}`;
            }
        } else {
            content = "There are no rewards for completing this achievement";
        }
        if (naStatus == 1) {
            if (!Achievements.reward(achieve.name, player)) {
                content = "#rYou are still unable to receive the rewards. Please make room in your inventory and try again.#k\r\n\r\n" + content;
            } else {
                delete section[selection];
                achieve.obj.setStatus(AStatus.RewardGiven);
            }
        }
        cm.sendNext(content);
        status = 1;
    }
}

function Achievement(name, obj) {
    this.name = name;
    this.obj = obj;
}