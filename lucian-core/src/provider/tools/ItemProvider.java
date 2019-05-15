package provider.tools;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;

/**
 * @author izarooni
 */
public class ItemProvider {

    private static final MapleDataProvider WZ = MapleDataProviderFactory.getWZ("Item.wz");

    private ItemProvider() {
    }

    public static MapleData getPet(int petID) {
        return WZ.getData(String.format("Pet/%d.img", petID));
    }
}
