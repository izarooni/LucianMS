// 9899943
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
        cm.sendGetText("Action");
    } else if (status == 2) {
        cm.showAnimation(parseInt(cm.getText()));
        status = 0;
        action(1, 0, 0);
    }
}
