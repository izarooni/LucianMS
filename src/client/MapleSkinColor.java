package client;

public enum MapleSkinColor {
    NORMAL(0), DARK(1), BLACK(2), PALE(3), BLUE(4), GREEN(5), WHITE(9), PINK(10), BROWN(11), RED(13);
    final int id;

    MapleSkinColor(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MapleSkinColor getById(int id) {
        for (MapleSkinColor l : MapleSkinColor.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }
}
