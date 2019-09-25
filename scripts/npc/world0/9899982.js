/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("You are beginning to feel different..You are beginning to remember your past. You are beginning to feel who you used to be.\r\n\r\nIt all makes sense now..I....\r\nI.......\r\n#eI...\r\n\r\n#eI AM \r\n#e...#r#h #", 1);
    } else if (status == 2) {
        player.changeMap(910000000);
        player.sendMessage("You have completed the tutorial. Welcome to Chirithy!");
        cm.dispose();
    }
}