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
        Phantom: 210,
        Luminous: 220,
        Evan: 230
    },
    Third: {
        Phantom: 211,
        Luminous: 221,
        Evan: 231
    },
    Fourth: {
        Phantom: 212,
        Luminous: 222,
        Evan: 232
    }
};
