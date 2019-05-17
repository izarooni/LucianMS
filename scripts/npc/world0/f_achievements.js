const SAchievements = Java.type("com.lucianms.io.scripting.Achievements");
/* izarooni */
let status = 0;
let achievements = {completed:[], other:[]};
let lastSelection;

player.getAchievements().entrySet().forEach(function(e) {
    let achieve = e.getValue();
    if (achieve.isCompleted()) achievements.completed.push([e.getKey(), achieve]);
    else achievements.other.push([e.getKey(), achieve]);
});

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let total = achievements.completed.length + achievements.other.length;
        let text = "You've completed #b" + achievements.completed.length + " of " + total + "#k discovered achievements\r\n#b";
        text += "\r\n#L0#Completed achievements#l";
        text += "\r\n#L1#Incomplete achievements#l"
        cm.sendSimple(text, 2);
    } else if (status == 2) {
        let text = "";
        lastSelection = selection;
        if (selection == 0) {
            if (achievements.completed.length == 0) {
                cm.sendOk("You have not completed any achievements");
                cm.dispose();
                return;
            }
            for (let i = 0; i < achievements.completed.length; i++) {
                let achieve = achievements.completed[i];
                text += "\r\n#L" + i + "##FUI/UIWindow/Memo/check1#  #b" + achieve[0] + "#l";
            }
        } else {
            if (achievements.other.length == 0) {
                cm.sendOk("There are no achievements");
                cm.dispose();
                return;
            }
            for (let i = 0; i < achievements.other.length; i++) {
                let achieve = achievements.other[i];
                text += "\r\n#L" + i + "##FUI/UIWindow/Memo/check0#  #r" + achieve[0] + "#l";
            }
        }
        cm.sendSimple(text, 2);
    } else if (status == 3) {
        let achieve = achievements[(lastSelection == 0) ? "completed" : "other"][selection];
        let content = "You " + ((lastSelection == 0) ? "received" : "will receieve") + ` the following thinigs for completing the achievement '#b${achieve[0]}'\r\n`;
        let rewards = SAchievements.getRewards(achieve[0]);
        if (rewards != null && rewards.size() > 0) {
            for (let reward in rewards) {
                content += `\r\n${rewards[reward]}`;
            }
        } else {
            content = "There are no rewards for completing this achievement";
        }
        cm.sendNext(content);
        status = 0;
    }
}
