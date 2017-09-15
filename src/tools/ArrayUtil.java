package tools;

/**
 * @author izarooni
 */
public class ArrayUtil {

    public ArrayUtil() {
    }

    public static boolean contains(int i, int... a) {
        for (long b : a) {
            if (b == i) {
                return true;
            }
        }
        return false;
    }
}
