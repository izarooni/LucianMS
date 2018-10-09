/* izarooni 
Gate of Magic
Map: 96
*/
load('scripts/npc/generic_jobadvance.js');
const FirstAdvancementGreet = "You hold an unending well filled with mana within your core. "
    + "The ceiling of power and success grows as you do. "
    + "An unstoppable force is what you can become, and nothing less awaits you.";
jobs = {
    First: {
        Magician: { ID: 200, req: (p) => p.getInt() >= 20,
            failMessage: "Make sure you have at least #b20#k points in your #bINT#k stat."}
    },
    Second: {
        Phantom  : { ID: 210 },
        Luminous : { ID: 220 },
        Evan     : { ID: 230 }
    },
    Third: {
        Phantom  : { ID: 211 },
        Luminous : { ID: 221 },
        Evan     : { ID: 231 }
    },
    Fourth: {
        Phantom  : { ID: 212 },
        Luminous : { ID: 222 },
        Evan     : { ID: 232 }
    }
};
