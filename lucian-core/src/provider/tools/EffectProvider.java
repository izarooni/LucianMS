package provider.tools;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class EffectProvider {

    private static final MapleDataProvider WZ = MapleDataProviderFactory.getWZ("Effect.wz");
    private static Map<Integer, Integer> SUMMON_EFFECT_CACHE;

    private EffectProvider() {
    }

    public static int getSummonEffect(int effectID) {
        MapleData data = WZ.getData("Summon.img");
        if (data == null) {
            return 0;
        } else if (SUMMON_EFFECT_CACHE == null) {
            SUMMON_EFFECT_CACHE = new HashMap<>(data.getChildren().size());
        }
        Integer sum;
        if ((sum = SUMMON_EFFECT_CACHE.get(effectID)) != null) {
            return sum;
        }

        MapleData effect = data.getChildByPath(Integer.toString(effectID));
        if (effect == null) {
            return 0;
        }
        sum = MapleDataTool.getInt("delay", effect, 0);
        for (int i = 0; ; i++) {
            MapleData canvas = effect.getChildByPath(Integer.toString(i));
            if (canvas == null) {
                break;
            }
            sum += MapleDataTool.getInt("delay", canvas, 0);
        }
        SUMMON_EFFECT_CACHE.put(effectID, sum);
        return sum;
    }
}
