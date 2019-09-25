/* izarooni
 JQ
 Warp NPC

 Fixed by Jeemie
*/

let status = 0;
let maps_easy = [
    // [6, "#bTetris #k"],
    [105040310, "#bThe Deep Forest of Patience <Step 1>"],
    [105040311, "#bThe Deep Forest of Patience <Step 2>"],
    [105040312, "#bThe Deep Forest of Patience <Step 3>"],
    [105040313, "#bThe Deep Forest of Patience <Step 4>"],
    [105040314, "#bThe Deep Forest of Patience <Step 5>"],
    [105040315, "#bThe Deep Forest of Patience <Step 6>"],
    [105040316, "#bThe Deep Forest of Patience <Step 7>"],
    [100000202, "#bPet-Walking Road"],
    [220000006, "#bLudibrium Pet Walkway"],
    [922020000, "#bThe Forgotten Darkness"]
];
let maps_medium = [
    [980044000, "#dWitch's Tower (1st Floor)"],
    [610020000, "#dThe Valley of Heroes"],
    [922240000, "#dRescue Gaga!"],
    [280020000, "#dThe Breath of Lava (Stage 1)"]
];
let maps_hard = [
    [682000200, "#rGhost Chimney"]
];

let lengthEnM = maps_easy.length + maps_medium.length;
let lengthEnMnH = maps_easy.length + maps_medium.length + maps_hard.length; 

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Hello, #h #!\r\nHere you can attempt the easiest or hardest challenges of your life.\r\n\r\nEvery jump quest completion rewards arcade coins. Depending on the difficulty, you will be rewarded different amounts of coins.\r\n\r\n#bEasy#k: 5 #barcade coins#k\r\n#dIntermediate#k: 7 #barcade coins#k\r\n#rHard#k: 10 #barcade coins#k\r\n\r\n#eEasy Difficulty#n:";
        for (let i = 0; i < maps_easy.length; i++) {
            let map_name = maps_easy[i][1];
            content += `\r\n#L${i}#${map_name}#l`;
        }
        content += "\r\n\r\n\r\n\r\n#e#kIntermediate Difficulty#n:";
        for (let i = maps_easy.length; i < lengthEnM; i++) {
            let map_name = maps_medium[i - maps_easy.length][1];
            content += `\r\n#L${i}#${map_name}#l`;
        }
        content += "\r\n\r\n\r\n\r\n#e#kHard Difficulty#n:";
        for (let i = lengthEnM; i < lengthEnMnH; i++) {
            let map_name = maps_hard[i - lengthEnM][1];
            content += `\r\n#L${i}#${map_name}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (selection >= 0 && selection < maps_easy.length) {
            let map_id = maps_easy[selection][0];
            cm.warp(map_id);
        }
        else if (selection >= maps_easy.length && selection < maps_easy.length + maps_medium.length) {
            let map_id = maps_medium[selection - maps_easy.length][0];
            cm.warp(map_id);
        }
        else if (selection >= lengthEnM && selection < lengthEnMnH) {
            let map_id = maps_hard[selection - lengthEnM][0];
            cm.warp(map_id);
        }
        cm.dispose();
    }
}
