package tools;

import java.util.Locale;
import java.util.Random;

/**
 * @author izarooni
 */
public class Randomizer {

    private static final Random rand = new Random();
    private static final char[] chars;

    static {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        str += str.toLowerCase(Locale.ROOT);
        str += "0123456789";
        chars = str.toCharArray();
    }

    /**
     * Delegate method for {@link Random#nextInt()}
     */
    public static int nextInt() {
        return rand.nextInt();
    }

    /**
     * Delegate method for {@link Random#nextInt(int)}
     */
    public static int nextInt(final int arg0) {
        return rand.nextInt(arg0);
    }

    /**
     * Delegate method for {@link Random#nextBytes(byte[])}
     */
    public static void nextBytes(final byte[] bytes) {
        rand.nextBytes(bytes);
    }

    /**
     * Delegate method for {@link Random#nextBoolean()}
     */
    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    /**
     * Delegate method for {@link Random#nextDouble()}
     */
    public static double nextDouble() {
        return rand.nextDouble();
    }

    /**
     * Delegate method for {@link Random#nextFloat()}
     */
    public static float nextFloat() {
        return rand.nextFloat();
    }

    /**
     * Delegate method for {@link Random#nextLong()}
     */
    public static long nextLong() {
        return rand.nextLong();
    }

    /**
     * Returns a random value between {@code lbound} value (inclusive) and the {@code ubound} value (inclusive)
     *
     * @param lbound the lower bound value
     * @param ubound the upper bound value
     * @return A random value between {@code lbound} and {@code ubound}
     */
    public static int rand(final int lbound, final int ubound) {
        return (int) ((rand.nextDouble() * (ubound - lbound + 1)) + lbound);
    }

    public static String nextString(int len) {
        char[] buffer = new char[len];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = chars[Randomizer.nextInt(chars.length)];
        }
        return new String(buffer);
    }
}