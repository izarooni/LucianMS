//FeinT
//Defeating Secret boss in Noragami
function start() {
    cm.sendSimple("In my darkest times of researching I figured out that the heart is just full of darkness, pain and anger. It seems like my research hasn't reached its maximum potential, yet.\r\n\r\nTake this #edark orb#k#n #i04011017#. as it contains your dear friend, #eNora's#k#n pure light. A vast darkness, yes. That is what everyone needs..\r\nWe will meet again, your time #eIS#k#n...");
}

function action(mode, type, selection) {
    cm.dispose();
    if (selection == 0) {
        cm.gainItem(4011017, 1);     //Noras darkness orb
        cm.warp(551030800, 0);
        cm.dispose();
    }
}
