package tools;

import java.util.Locale;
import java.util.Random;

public class Randomizer {

    private static final Random rand = new Random();
    private static final char[] chars;

    static {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        str += str.toLowerCase(Locale.ROOT);
        str += "0123456789";
        chars = str.toCharArray();
    }

    public static int nextInt() {
        return rand.nextInt();
    }

    public static int nextInt(final int arg0) {
        return rand.nextInt(arg0);
    }

    public static void nextBytes(final byte[] bytes) {
        rand.nextBytes(bytes);
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static double nextDouble() {
        return rand.nextDouble();
    }

    public static float nextFloat() {
        return rand.nextFloat();
    }

    public static long nextLong() {
        return rand.nextLong();
    }

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