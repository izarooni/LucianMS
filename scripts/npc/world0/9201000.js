load('scripts/util_wedding.js');
/* izarooni 
Moony
*/
let status = 0;
cm.vars = { completed: false };

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let quest = player.getCustomQuest(POL_HENE);
    if (quest == null) return NotStarted();
    else if (cm.vars.completed) return QuestCompleted(selection);
    else {
        let completed = 0;
        for (let i = 0; i < POL_VICTORIA.length; i++) {
            let quest = player.getCustomQuest(POL_VICTORIA[i]);
            if (quest != null && quest.isCompleted()) {
                completed++;
            }
        }
        if (completed >= 4) {
            cm.vars.completed = true;
            status = 0;
            action(1, 0, 0);
        } else {
            cm.sendOk("Bring me back four color #bProof of Loves#k. You can get them from talking to #bNana the Love Fairy#k in any town.");
            cm.dispose();
        }
    }
}

function QuestCompleted(selection) {
    if (status == 1) {
        if (player.isDebug()) {
            for (let i = 0; i < POL_VICTORIA.length; i++) {
                cm.gainItem(POL_Items[i], 1);
            }
        }
        cm.sendNext("Marvelous! The show of dedication to obtain these Proof of Love. I'm getting super excited for the day of the marriage! Before that, you will need to get engaged. Please hand over the four different colored Proof of Love");
    } else if (status == 2) {
        cm.sendSimple("Tell me, which engagement type of ring would you like?\r\n"
        + "\r\n#b#L0#Moonstone#l"
        + "\r\n#b#L1#Star gem#l"
        + "\r\n#b#L2#Golden Heart#l"
        + "\r\n#b#L3#Silver Swan#l");
    } else if (status == 3) {
        cm.vars.selection = selection;
        let content = `Are you sure you want to use this ring for your marriage?`
        + `\r\n\t\t\t\t\t\t#v${WED_RINGS[selection]}# #b#z${WED_RINGS[selection]}#`
        + `\r\n\r\n\t\t\t\t\t\t\t  ${WED_EFFECT[selection]}`;
        cm.sendYesNo(content);
    } else if (status == 4) {
        for (let i = 0; i < POL_Items.length; i++) {
            cm.gainItem(POL_Items[i], -1);
        }

        cm.gainItem(ENG_BOXES[cm.vars.selection]);
        cm.sendOk("Here is your engagement ring. I have placed it nicely in a beautiful box. When the time is right, use this to propose to your significant other");
        cm.dispose();
    }
}

function ForceForfeit() {
    for (let i = 0; i < POL_VICTORIA.length; i++) {
        let quest = player.getCustomQuest(POL_VICTORIA[i]);
        if (quest != null) {
            player.getCustomQuests().remove(quest.getId());
        } 
    }
    cm.sendOk("Quests forfeited");
    cm.dispose();
}

function NotStarted() {
    if (status == 1) {
        cm.sendYesNo("Hey, you look like you might want to be married! Want to make an engagement ring?");
    } else if (status == 2) {
        for (let i = 0; i < POL_VICTORIA.length; i++) {
            CQuests.beginQuest(player, POL_VICTORIA[i], true);
        }
        cm.sendOk("Okay, first bring me back any four colored #bProof of Loves#k. You can get them from talking to #bNana the Love Fairy#k in any town. Also, only one of you, either the Groom or Bride will have to do this quest.");
        cm.dispose();
    }
}