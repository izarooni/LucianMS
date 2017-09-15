package tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author izarooni
 */
public class Tester {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tester.class);

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            System.out.println(Randomizer.nextString(8));
        }
    }
}
