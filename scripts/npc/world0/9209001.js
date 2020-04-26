const SkillFactory = Java.type('com.lucianms.client.SkillFactory');

let status = 0;
var skill = SkillFactory.getSkill(3101003);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        player.changeSkillLevel(skill, 10, 10, -1);
		    if (player.getSkill(3101003) == true) {
          cm.sendOk("works");
        }
        else {
          cm.sendOk("test");
        }
        cm.dispose();
    }
}
