package tools;

/**
 * @author izarooni
 */
public class ArrayUtil {

    public ArrayUtil() {
    }

    /**
     * Does a linear search in a {@code int[]} type array
     *
     * @param i value to search for
     * @param a the array of integers
     *
     * @return if the value was found in the array
     */
    public static boolean contains(int i, int... a) {
        for (long b : a) {
            if (b == i) {
                return true;
            }
        }
        return false;
    }

    public static byte[] reverse(byte[] b) {
        byte[] ret = new byte[b.length];
        for (int dest = 0, src = b.length; src > 0; src--) {
            ret[dest++] = b[src - 1];
        }
        return ret;
    }
}
