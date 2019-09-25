package provider.tools;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import tools.StringUtil;

/**
 * @author izarooni
 */
public class SkillDataProvider {

    private static MapleDataProvider WZ = MapleDataProviderFactory.getWZ("Skill.wz");
    private static MapleData MOB_SKILLs;

    private SkillDataProvider() {
    }

    public static MapleDataProvider getProvider() {
        return WZ;
    }

    public static MapleData getJob(int jobID) {
        return WZ.getData(String.format("%s.img", StringUtil.getLeftPaddedStr(Integer.toString(jobID), '0', 3)));
    }

    public static MapleData getMobSkills() {
        if (MOB_SKILLs == null) {
            MOB_SKILLs = WZ.getData("MobSkill.img");
        }
        return MOB_SKILLs;
    }
}
