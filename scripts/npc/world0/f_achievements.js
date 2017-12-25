/* izarooni */
var status = 0;

var achievements = {completed:[], other:[]};

player.getAchievements().entrySet().forEach(function(e) {
    var achieve = e.getValue();
    if (achieve.isCompleted()) achievements.completed.push([e.getKey(), achieve]);
    else achievements.other.push([e.getKey(), achieve]);
});

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var total = achievements.completed.length + achievements.other.length;
        var text = "You've completed #b" + achievements.completed.length + "/" + total + "#k achievements\r\n#b";
        text += "\r\n#L0#Completed achievements#l";
        text += "\r\n#L1#Incomplete achievements#l"
        cm.sendSimple(text, 2);
    } else if (status == 2) {
        var text = "";
        if (selection == 0) {
            for (var i = 0; i < achievements.completed.length; i++) {
                var achieve = achievements.completed[i];
                text += "\r\n#FUI/UIWindow/Memo/check1#  #b" + achieve[0];
            }
        } else {
            for (var i = 0; i < achievements.other.length; i++) {
                var achieve = achievements.other[i];
                text += "\r\n#FUI/UIWindow/Memo/check0#  #r" + achieve[0];
            }
        }
        cm.sendNext(text, 2);
        status = 0;
    }
}
