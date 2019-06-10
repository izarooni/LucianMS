load('scripts/util_wedding.js');
/* izarooni 
High Priest John
*/
// event script manger, event script manager instance name
const ESM = "CathedralWedding", ESMIN = `SaintAltar-${ch.getId()}`;
const nFieldAltar = 680000210;
const nCurrencyCost = 5;
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (player.getMapId() != nFieldAltar) {
        let quest = player.getCustomQuest(POL_PARENTS);
        if (quest == null || !quest.isCompleted()) return NoQuest(quest);
        else if (quest.isCompleted() && cm.haveItem(PARENTS_BLESSING)) return QuestCompleted();
        else if (cm.haveItem(PRIESTS_PERMISSION)) return BeginWedding();
    } else {
        let em = ch.getEventScriptManager().getManager(ESM);
        if (em != null) {
            let eim = em.getInstance(ESMIN);
            if (eim != null) return DoWedding(eim);
        } else {
            cm.sendOk("The altar is currently unavailable.");
        }
        cm.dispose();
    }
}

function DoWedding(eim) {
    let sn = eim.getProperty("s"), on = eim.getProperty("o");
    if (status == 1) {
        cm.sendNext("We are gathered here today to witness and celeberate the formal joining of "
        + `#b${sn}#k and #b${on}#k in the legal state of Matrimony.`);
    } else if (status == 2) {
        cm.sendNext("As you begin your marriage, let your home be a haven of peace in the midst of a busy and changing world; Let your relationship be one of complete truth, love and understanding between you both. Respect the confidences of your partner in this marriage, and consider that your marriage will yield only what you give it.");
    } else if (status == 3) {
        cm.sendOk("If you treat each other with kindness, with compassion and trust, and always let your tender feelings show: If you laugh together often and enjoy the time you share, but give each other space to learn and grow: If you understand your differences, respect who yo uare, and put each other first in all you do --  Your marriage will be beautiful, a reason to feel proud, and a special source of love your whole lives through...\r\nMay you enjoy a long life, fulfill your hopes and dreams, feel content as you live day by day, and keep your promises to each other.....");
        cm.dispose();
    }
}

function BeginWedding() {
    let em = ch.getEventScriptManager().getManager(ESM);
    if (status == 1) {
        if (em != null) {
            let eim = em.getInstance(ESMIN);
            if (eim == null) {
                cm.sendNext("My wonderful assistants and I are ready to have your special event in the Cathedral. With your significant other, are you ready for your wedding?");
            } else {
                if (player.isDebug()) {
                    eim.dispose();
                    em.removeInstance(eim.getName());
                    cm.sendOk("The event instance for this channel has been disposed");
                } else {
                    cm.sendNext("There is a wedding currently ongoing in the Cathedral");
                }
                cm.dispose();
            }
        } else {
            cm.sendOk("The Cathedral is not available right now");
            cm.dispose();
        }
    } else if (status == 2) {
        let party = cm.getParty();
        if (party == null) 
            cm.sendOk("Please form a party with your significant other");
        else if (party.size() != 2)
            cm.sendOk("The only person in your party should be your significant other. Please expel any other persons from the party as they will need to join their other wedding invitees.");
        else if (player.getRelationship().getStatus().ordinal() != 1) {
            cm.sendOk("How are you planning to have a wedding if you are not engaged?");
        } else {
            let members = cm.getPartyMembers();
            let s = members.get(0), o = members.get(1);
            if (s.getMap() != o.getMap()) {
                cm.sendOk("Is your significant other nearby? We can't start the wedding unless you are both here");
                return cm.dispose();
            }
            let sr = s.getRelationship(), or = o.getRelationship();
            if ((sr.getGroomId() == o.getId() || sr.getBrideId() == o.getId()) &&
                (or.getGroomId() == s.getId() || or.getBrideId() == s.getId())) {
                if (cm.isLeader()) {
                    let eim = em.getInvocable().invokeFunction("setup")
                    eim.setProperty("s", s.getName());
                    eim.setProperty("o", o.getName());
                    eim.registerPlayer(s);
                    eim.registerPlayer(o);
                    player.getClient().getWorldServer().broadcastMessage(6, "A wedding will be starting in the Cathedral at channel {}", ch.getId());
                    cm.gainItem(PRIESTS_PERMISSION, -1);
                } else {
                    cm.sendOk("Only the party leader may begin the wedding");
                }
            } else {
                cm.sendOk("Please form a party with your significant other");
            }
        }
        cm.dispose();
    }
}

function QuestCompleted() {
    if (status == 1) {
        cm.sendYesNo("The Cathedral will need to be reserved for your wedding and will cost #b"
        + `${nCurrencyCost} #b#z${Constants.CURRENCY}##k. I will give you my proof of permission to have your wedding here.\r\nAre you ready to pay right now?`);
    } else if (status == 2) {
        if (cm.haveItem(Constants.CURRENCY, nCurrencyCost)) {
            cm.gainItem(Constants.CURRENCY, -nCurrencyCost);
            cm.gainItem(PARENTS_BLESSING, -1);
            cm.gainItem(PRIESTS_PERMISSION, 1, true);
            cm.sendOk("Speak to me again once you are ready to begin your wedding");
            cm.dispose();
        } else {
            cm.sendOk(`You do not have enough #z${Constants.CURRENCY}#`);
            cm.dispose();
        }
    }
}

function NoQuest(quest) {
    if (quest == null) {
        cm.sendOk("So you've found your true love? I will officiate your wedding if you can get #bMom & Dad's#k blessing");
    } else {
        cm.sendOk("You are almost at the final stages of your engagement! Please get #bMom and Dad's#k blessing for me.");
    }
    cm.dispose();
}