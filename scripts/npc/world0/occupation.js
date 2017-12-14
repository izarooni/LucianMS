var Occupation = Java.type("client.meta.Occupation");
/* izarooni */
var status = 0;

var invoke = null;
var pocc = player.getOccupation();

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 0 ){
        if (invoke == Career) {
            status = 2;
            invoke = Beginner;
        }
    } else {
        status++;
    }
    if (invoke == null) {
        if (pocc == null) {
            invoke = Beginner;
        } else {
            // maximum lazy
            Career(pocc.getType().ordinal());
            cm.dispose();
            return;
        }
        if (invoke == null) {
            cm.sendOk("\"hmmm\" - #bizarooni#k");
            cm.dispose();
            return;
        }
    }
    invoke(selection);
}

function Beginner(selection) {
    if (status == 1) {
        cm.sendNext("You haven't decided on your occupation?\r\nThere's a few you can pick from, let me tell you about them");
    } else if (status == 2){
        cm.sendSimple("There's #bPharoah, Undead, Demon and Human#k and each one has it's own attributes that may help with your endeavors. Which one would you like to learn about?\r\n#b"
            + "\r\n#L0#Pharoah#l"
            + "\r\n#L1#Undead#l"
            + "\r\n#L2#Demon#l"
            + "\r\n#L3#Human#l");
        status = 0;
        invoke = Career;
    }
}

function Career(selection) {
    if (status == 1) {
        this.career = selection;
        if (selection == 0) {
            cm.sendNextPrev("The #bPharaoh#k has a higher EXP rate (1+), but a lower meso & drop rate (-1)\r\n\r\n\t#bGood for people with no life");
        } else if (selection == 1) {
            cm.sendNextPrev("The #bUndead#k gains HP each time a monster is attacked and does not lose EXP upon death\r\n\r\n\t#bGood for bossing & leeching");
        } else if (selection == 2) {
            cm.sendNextPrev("The #bDemon#k has faster running speed (+5), and is immune to all diseases from monsters\r\n\r\n\t#bGood for bossing");
        } else if (selection == 3) {
            cm.sendNextPrev("The #bHuman#k has a higher loot rate and meso rate (1+)\r\n\r\n\t#bGood for farming")
        }
    } else if (status == 2) {
        cm.sendYesNo("Are you sure you have decided to become a #b" + NameFromValue(this.career) + "#k?");
    } else if (status == 3) {
        player.setOccupation(new Occupation(TypeFromValue(this.career)));
        cm.sendOk("You are now a #b" + NameFromValue(this.career) + "#k")
        cm.dispose();
    }
}

function TypeFromValue(n) {
    switch (n) {
        default: return null;
        case 0: return Occupation.Type.Pharaoh;
        case 1: return Occupation.Type.Undead;
        case 2: return Occupation.Type.Demon;
        case 3: return Occupation.Type.Human;
    }
}

function NameFromValue(n) {
    switch (n) {
        default: return null;
        case 0: return "Pharaoh";
        case 1: return "Undead";
        case 2: return "Demon";
        case 3: return "Human";
    }
}
