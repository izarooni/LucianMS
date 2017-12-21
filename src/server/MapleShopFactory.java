package server;

import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class MapleShopFactory {

    private static final MapleShopFactory instance = new MapleShopFactory();
    private Map<Integer, MapleShop> shops = new HashMap<>();
    private Map<Integer, MapleShop> npcShops = new HashMap<>();

    public static MapleShopFactory getInstance() {
        return instance;
    }

    public void clearCache() {
        shops.clear();
        npcShops.clear();

        shops = new HashMap<>();
        npcShops = new HashMap<>();

        System.gc();
    }

    public MapleShop getShop(int shopId) {
        return shops.computeIfAbsent(shopId, id -> MapleShop.createFromDB(shopId, true));
    }

    public MapleShop getShopForNPC(int npcId) {
        return npcShops.computeIfAbsent(npcId, id -> MapleShop.createFromDB(npcId, false));
    }
}
