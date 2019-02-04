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
            failMessage: "Make sure you have at least #b20#k points in your #bINT#k stat.",
            equips: [[1382000, 1]]}
    },
    Second: {
        "Demon Slayer" : { ID: 210 },
        Luminous: { ID: 220 },
        Evan: { ID: 230 }
    },
    Third: {
        "Demon Slayer" : { ID: 211 },
        Luminous: { ID: 221 },
        Evan: { ID: 231 }
    },
    Fourth: {
        "Demon Slayer": { ID: 212, 
            skills: [[2121000, 30], [2121003, 30], [2121004, 30], [2121005, 30], [2121007, 30], [2121008, 5]] },
        Luminous: { ID: 222, 
            skill: [[2221000, 30], [2221003, 30], [2221004, 30], [2221005, 30], [2221007, 30], [2221008, 5]] },
        Evan: { ID: 232, 
            skills: [[2321000, 30], [2321003, 30], [2321004, 30], [2321006, 30], [2321007, 30], [2321008, 30], [2321009, 5]] }
    }
};
