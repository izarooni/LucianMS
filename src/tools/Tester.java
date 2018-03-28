package tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.maps.MapleReactorFactory;
import server.maps.MapleReactorStats;

/**
 * @author izarooni
 */
public class Tester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tester.class);

    public static void main(String[] args) {
        System.setProperty("wzpath", "wz");
        MapleReactorStats reactor = MapleReactorFactory.getReactor(9702000);
        System.out.println(reactor == null ? "nah" : "null");
    }
}
