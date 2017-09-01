package tools;

import client.MapleCharacter;
import server.life.FakePlayer;

/**
 * @author izarooni
 */
public class Tester {

    public static void main(String[] args) {
        FakePlayer fakePlayer = new FakePlayer("test");
        test(fakePlayer);
    }

    public static void test(MapleCharacter test){
        test.getClient().announce(MaplePacketCreator.enableActions());
    }
}
