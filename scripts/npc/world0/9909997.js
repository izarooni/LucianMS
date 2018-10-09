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
            failMessage: "Make sure you have at least #b25#k points in your #bLUK#k stat." }
    },
    Second: {
        Shade: 410,
        "Dual Blader": 420
    },
    Third: {
        Shade: 411,
        "Dual Blader": 421
    },
    Fourth: {
        Shade: 412,
        "Dual Blader": 422
    }
};
