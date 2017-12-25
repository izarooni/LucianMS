//By Venem
//Defeat Genos reward
function start() {
cm.sendSimple ("#rWait#k...\r\n\r\nI sense that I was being controlled.\r\nIt must have been the #rBlack Mage#k Please, take this mask! It will make you stronger and able to defeat him once and for all!\r\nGood luck my friends.\r\n#L0##i1012299##bTake Mask#k");
}

function action(mode, type, selection) {
cm.dispose();
         if (selection == 0) {
	cm.gainItem(1012299, 1);     //Mask of the Ghoul
    cm.gainItem(1012498, 1);     //Kanekis mask
cm.warp(86, 0);
	cm.dispose();
}
}
