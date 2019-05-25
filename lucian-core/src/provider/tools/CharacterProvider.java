package provider.tools;

import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;

/**
 * @author izarooni
 */
public class CharacterProvider {

    private static final MapleDataProvider WZ = MapleDataProviderFactory.getWZ("Character.wz");

    private CharacterProvider() {
    }

    public static MapleDataProvider getProvider() {
        return WZ;
    }
}
