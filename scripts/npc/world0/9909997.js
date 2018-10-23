/* izarooni
Gate of Darkness
Map: 96
 */
load('scripts/npc/generic_jobadvance.js');
const FirstAdvancementGreet = "Your presence is weak. It was meant to be, your future of darkness. "
    + "Become the nightmare as fear grows in darkness and rule all of LucianMS.";
jobs = {
    First: {
        Rogue: { ID: 400, req: (p) => p.getLuk() >= 25,
            failMessage: "Make sure you have at least #b25#k points in your #bLUK#k stat.",
            equips: [[1472000, 1], [1332007, 1], [2070000, 1000]] },
        "Night Walker": { ID: 1400, req: (p) => p.getLuk() >= 25,
            failMessage: "Make sure you have at least #b25#k points in your #bLUK#k stat.",
            equips: [[1472000, 1], [1332007, 1], [2070000, 1000]] }
    },
    Second: {
        Dancer: { ID: 410 },
        "Dual Blader" : { ID: 420 },
        "Night Walker" : { ID: 1410 }
    },
    Third: {
        Dancer: { ID: 411 },
        "Dual Blader" : { ID: 421 },
        "Night Walker" : { ID: 1411, skills: [[14110004, 30], [14111005, 30]] }
    },
    Fourth: {
        Dancer: { ID: 412,
            skills: [[4121003, 30], [4121004, 30], [4121007, 30], [4121008, 30], [4121009, 5]] },
        "Dual Blader" : { ID: 422,
            skills: [[4220002, 30], [4221007, 30], [4221000, 30], [4221001, 30], [4221003, 30], [4221004, 30], [4221006, 30], [4221008, 5]] },
        "Night Walker": { ID: 1412 }
    }
};
