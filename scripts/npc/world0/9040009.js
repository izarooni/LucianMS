load('scripts/util_gpq.js');
/* izarooni */
let status = 0;
let eim = player.getEventInstance();

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (eim == null) return removePlayer();
    else if (cm.isLeader()) return VerifyPattern();
}

function VerifyPattern() {
    if (eim.vars.stage == undefined) {
        cm.sendNext("In this challenge, I shall show a pattern on the statues around me. When I give the word, repeat the pattern to me to proceed.");
        eim.vars.stage = 1;
        cm.dispose();
        return;
    }
    
    if (player.isDebug()) eim.vars.statueStage = 3;

    let stage = eim.vars.statueStage;
    let pattern = eim.vars.statuePatterns[stage];

    if (stage == 3) {
        let gate = player.getMap().getReactorByName("statuegate");
        if (gate.getState() == 0) {
            gate.hitReactor(client);
            cm.getGuild().gainGP(15);
        } else {
             cm.sendOk("I have already opened the gate to the fortress for you.");
             cm.dispose();
         }
    } else if (eim.vars.enteredPattern.length > 0) {
        let entered = eim.vars.enteredPattern;
        let matches = true;
        for (let i = 0; i < entered.length; i++) {
            if (entered[i] != pattern[i]) {
                break;
            }
            matches = false;
        }
        eim.vars.enteredPattern = [];
        if (matches) {
            cm.showEffect("quest/party/clear");
            cm.playSound("Party1/Clear");
            cm.sendNext("You must be the wise one. That was the correct answer!\r\nNext stage.");
            eim.vars.statueStage++;
            eim.vars.enteredPattern = [];
            status = 0;
        } else {
            eim.vars.statueStage = 0;
            eim.vars.statuePatterns = [];
            cm.showEffect("quest/party/wrong_kor");
            cm.playSound("Party1/Failed");
            cm.sendNext("That was the incorrect answer.\r\nRestart.")
        }
    } else if (pattern != undefined) {
        cm.sendOk("I have already given you the pattern.");
        cm.dispose();
    } else if (status == 1) {
        cm.sendNext("I shall now present a more difficult puzzle for you. Good luck.");
    } else if (status == 2) {
        eim.schedule("CreateStatueCombo", 1000);
        cm.dispose();
    }
}

function removePlayer() {
    if (status == 1) {
        cm.sendNext("You should not be here");
    } else  {
        player.changeMap(nFieldConstructionSite);
        cm.dispose();
    }
}