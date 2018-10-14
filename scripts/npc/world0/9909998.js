/* izarooni
Gate of Swiftness
Map: 96
 */
load('scripts/npc/generic_jobadvance.js');
const FirstAdvancementGreet = "Meet absolute strength in a single moment. "
    + "Your every goal can be within grasp and obtained should you pursue. "
    + "Leave those whom fall to weakness lift you above the clouds.";
jobs = {
    First: {
        Archer: { ID: 300, req: (p) => p.getDex() >= 25,
            failMessage: "Make sure you have at least #b25#k points in your #bDEX#k stat.",
            equips: [[1452002, 1], [2060000, 2000]] }
    },
    Second: {
        Mercedes       : { ID: 310 },
        "Crossbow Man" : { ID: 320 }
    },
    Third: {
        Mercedes       : { ID: 311 },
        "Sniper" : { ID: 321 }
    },
    Fourth: {
        Mercedes       : { ID: 312 },
        "Marksman" : { ID: 322 }
    }
};
