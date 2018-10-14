/* izarooni
Gate of Power
Map: 96
 */
load('scripts/npc/generic_jobadvance.js');
const FirstAdvancementGreet = "Your presence is weak but I sense great potential from you, adventurer. "
+ "I can lend you strength which matches god. A power that will allow you to overcome the greatest obstacles. "
+ "But only you can make that decision.";

jobs = {
    First: {
        Warrior: { ID: 100, req: (p) => p.getStr() >= 35,
            failMessage: "Make sure you have at least #b35#k points in your #bSTR#k stat.",
            equips: [[1302007, 1]] },
        Pirate: { ID: 500, req: (p) => true,
            equips: [[1482000, 1], [1492000, 1], [2330000, 1000]] },
        Aran: { ID: 2100, req: (p) => true,
            equips: [[1442000, 1]]},
        "Dawn Warrior": { ID: 1100, req: (p) => true,
            equips: [[1302007, 1]] }
    },
    Second: {
        Uchiha: { ID: 110 },
        Rashoumon: { ID: 120 },
        Ark: { ID: 510 },
        Mechanic: { ID: 520 },
        Aran: { ID: 2110 },
        "Dawn Warrior": { ID: 1110 }
    },
    Third: {
        Uchiha: { ID: 111 },
        Rashoumon: { ID: 121 },
        Ark: { ID: 511 },
        Mechanic: { ID: 521 },
        Aran: { ID: 2111 },
        "Dawn Warrior": { ID: 1111 }
    },
    Fourth: {
        Uchiha: { ID: 112 },
        Rashoumon: { ID: 122 },
        Ark: { ID: 512 },
        Mechanic: { ID: 522 },
        Aran: { ID: 2112 }
    }
};