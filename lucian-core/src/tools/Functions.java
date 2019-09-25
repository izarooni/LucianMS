package tools;

import java.util.function.Consumer;

/**
 * god i love in-line code
 *
 * @author izarooni
 */
public class Functions {

    private Functions() {
    }

    public static <T> void requireNotNull(T t, Consumer<T> consumer) {
        if (t != null) {
            consumer.accept(t);
        }
    }
}
