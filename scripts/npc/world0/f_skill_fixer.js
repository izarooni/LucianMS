const SkillFactory = Java.type('com.lucianms.client.SkillFactory');
const skills = {
    "112": [[1121008, 30], [1120003, 30], [1120004, 30], [1120005, 30], [1121000, 30], [1121001, 30], [1121002, 30], [1121006, 30], [1121010, 30], [1121011, 5]],
    "222": [[2221000, 30], [2221003, 30], [2221004, 30], [2221005, 30], [2221007, 30], [2221008, 5]],
    "312": [[3121000, 30], [3121003, 30], [3121004, 30], [3120005, 30], [3121006, 30], [3121008, 30], [3121009, 5]],
    "322": [[3221000, 30], [3221001, 30], [3221003, 30], [3221004, 30], [3221005, 30], [3221007, 30], [3221008, 30], [4121000, 30]],
    "422": [[4221000, 30], [4220002, 30], [4221007, 30], [4221001, 30], [4221003, 30], [4221004, 30], [4221006, 30], [4221008, 5]],
    "512": [[5121000, 30], [5121003, 30], [5121004, 30], [5121005, 30], [5121008, 30], [5121010, 30]],
    "1411": [[14110004, 20], [14111005, 20]]
};
/* izarooni 
temporary NPC
*/
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendOk("Your skills have been fixed.");
        let jobID = player.getJob().getId();
        LearnSkills(skills[jobID - 1]); 
        LearnSkills(skills[jobID]);
        cm.dispose();
    }
}

const LearnSkills = function (skills) {
    if (skills == null) return;
    for (let i = 0; i < skills.length; i++) {
        let skillID = skills[i][0];
        let maxLevel = skills[i][1];
        let skill = SkillFactory.getSkill(skillID);
        if (skill != null) {
            player.changeSkillLevel(skill, player.getSkillLevel(skill), maxLevel, -1);
        }
    }
};