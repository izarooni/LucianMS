package tools;

/**
 * LUL
 *
 * @author izarooni
 */
public class Triple<L, M, R> {

    private L left;
    private M middle;
    private R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("Triple{left=%s, middle=%s, right=%s}", left.toString(), middle.toString(), right.toString());
    }

    public L getLeft() {
        return left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public M getMiddle() {
        return middle;
    }

    public void setMiddle(M middle) {
        this.middle = middle;
    }

    public R getRight() {
        return right;
    }

    public void setRight(R right) {
        this.right = right;
    }
}
