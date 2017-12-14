importPackage(Packages.tools);
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");

/* dosent work waaahhhh */
/* player npc buged */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Yo, Big Shaq\r\nthe one and only Man's not hot\r\nnever hot Skrrat\r\nskidi-kat-kat\r\nBoom");
    } else if (status == 2) {
        cm.sendNext("Two plus two is four, minus one that's three, quick maths\r\nEveryday man's on the block, smoke trees\r\nSee your girl in the park, that girl is a uckers\r\nWhen the ting went quack-quack-quack, you man were ducking\r\n(you man ducked)\r\nHold tight, Asznee (my brudda), he's got the pumpy (big ting)\r\nHold tight, my man (my guy), he's got the frisbee\r\nI trap, trap, trap on the phone, movin' that cornflakes\r\nRice Krispies, hold tight my girl Whitney (my G)\r\nOn the road doin' ten toes, like my toes (like my toes)\r\nYou man thought I froze, I see a peng girl, then I pose (chilin')\r\nIf she ain't on it, I ghost, hah, look at your nose (check your nose, fam)\r\nYou donut, nose long like garden hose");
    } else if (status == 3) {
        cm.sendNext("I tell her man's not hot, I tell her man's not hot\r\nThe girl told me, 'Take off your jacket'\r\nI said 'Babes, mans not hot' (never hot)\r\nI tell her man's not hot (never hot)\r\nI tell her man's not hot (never hot)\r\nThe girl told me, 'Take off your jacket'\r\nI said 'Babes, mans not hot' (never hot)");
    } else if (status == 4) {
        cm.sendNext("Hop out the four-door with the .44, it was one, two, three and four (us man)\r\nChillin' in the corridor (yo), your dad is forty-four\r\nAnd he's still callin' man for a draw (look at him), let him know\r\nWhen I see him, I'm gonna spin his jaw (finished)\r\nTake man's Twix by force (take it), send man's shop by force (send him)\r\nYour girl knows I've got the sauce (flexin'), no ketchup (none) Just sauce(sauce), rawsauce\r\nAh, yo, boom, ah");
    } else if (status == 5) {
        cm.sendNext("The ting goes skrrrahh, pap, pap, ka-ka-ka\r\nSkidiki-pap-pap, and a pu-pu-pudrrrr-boom\r\nSkya, du-du-ku-ku-dun-dun\r\nPoom, poom, you don' know");
    } else if (status == 6) {
        cm.sendNext("Man can never be hot (never hot), perspiration ting (spray dat)\r\n(come on), you didn't hear me, did you? (nah)\r\nUse roll-on (use that), or spray\r\nBut either way, A-B-C-D");
    } else if (status == 6) {
        cm.sendNext("The ting goes skrrrahh, pap, pap, ka-ka-ka\r\nSkidiki-pap-pap, and a pu-pu-pudrrrr-boom\r\nSkya, du-du-ku-ku-dun-dun\r\nPoom, poom, you don' know");
    } else if (status == 6) {
        cm.sendNext("Big Shaq, man's not hot\r\nI tell her man's not hot (never hot)\r\n40 degrees and man's not hot (come on)\r\nYo, in the sauna, man's not hot (never hot)\r\nYeah, skidika-pap-pap");
        cm.dispose();
        cm.changeMusic("Bgm0/BigShaq");
    }
}
