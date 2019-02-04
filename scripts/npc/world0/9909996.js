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
            equips: [[1442000, 1]], skills: [[21000000, 10], [21001001, 20], [21001003, 20]]},
        Valkyrie: { ID: 1100, req: (p) => p.getStr() >= 35,
            failMessage: "Make sure you have at least #b35#k points in your #bSTR#k stat.",
            equips: [[1302007, 1]] }
    },
    Second: {
        Uchiha: { ID: 110 },
        Rashoumon: { ID: 120 },
        Dragoon: { ID: 130 },
        Ark: { ID: 510 },
        Mechanic: { ID: 520 },
        Aran: { ID: 2110, skills: [[21100000, 20], [21100002, 30], [21100004, 20], [21100005, 20]] },
        Valkyrie: { ID: 1110 }
    },
    Third: {
        Uchiha: { ID: 111 },
        Rashoumon: { ID: 121 },
        Dragoon: { ID: 131 },
        Ark: { ID: 511 },
        Mechanic: { ID: 521 },
        Aran: { ID: 2111 },
        Valkyrie: { ID: 1111 }
    },
    Fourth: {
        Uchiha: { ID: 112,
            skills: [[1121008, 30], [1120004, 30], [1120003, 30], [1120005, 30], [1121000, 30], [1121001, 30], [1121002, 30], [1121006, 30], [1121010, 30], [1121011, 5]] },
        Rashoumon: { ID: 122,
            skills: [[1220006, 30], [1220010, 30], [1221000, 30], [1221002, 30], [1221003, 30], [1221004, 30], [1221007, 30], [1221011, 30], [1221012, 5]] },
        Dragoon: { ID: 132,
            skills: [[1320006, 30], [1320008, 30], [1320009, 30], [1321000, 30], [1321002, 30], [1321003, 30], [1321010, 5]] },
        Ark: { ID: 512,
            skills: [[5121000, 30], [5121003, 30], [5121004, 30], [5121005, 30], [5121008, 30], [5121010, 30]] },
        Mechanic: { ID: 522,
            skills: [[5221000, 30], [5221003, 30], [5221006, 30], [5221007, 30], [5221008, 30], [5221009, 30], [5221010, 5]] },
        Aran: { ID: 2112,
            skills: [[21120004, 30], [21120005, 30], [21120006, 30], [21120007, 30], [21121000, 30], [21121008, 5]] }
    }
};